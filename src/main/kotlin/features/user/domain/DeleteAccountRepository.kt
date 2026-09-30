package com.ducks.features.user.domain

import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.user.data.dto.AccountDeletionDTO
import com.ducks.features.user.database.UserTable
import com.ducks.util.DucksBadRequestError
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class DeleteAccountRepository(
    private val confirmations: AccountDeletionConfirmations,
) {

    /**
     * Удаляет аккаунт клиента после подтверждения кодом из СМС. Необратимо: восстановления
     * нет, вход по тому же номеру создаст новый аккаунт с нуля.
     *
     * Доступ закрывается сразу — валидатор токена не найдёт пользователя, и все выданные
     * ему JWT перестанут проходить проверку. Остатки персональных данных подчищает
     * [com.ducks.features.user.service.AnonymizeDeletedUsersService] по истечении
     * [ACCOUNT_DELETION_GRACE_PERIOD].
     */
    suspend fun requestDeletion(userId: Long, confirmationToken: String?): AccountDeletionDTO {
        // Одного JWT мало: удаление подтверждается кодом из СМС, обменянным на токен.
        if (confirmationToken == null || !confirmations.isConfirmed(userId, confirmationToken)) {
            throw DucksBadRequestError("Подтвердите удаление кодом из СМС")
        }

        return newSuspendedTransaction {
            val user = UserTable
                .select(UserTable.phoneNumber)
                .where { UserTable.id eq userId }
                .firstOrNull()
                ?: throw DucksBadRequestError("Аккаунт не найден")

            // Заказ в работе оставлять нельзя: продавец останется с заказом от клиента,
            // которому уже некуда позвонить.
            val hasActiveOrder = CoffeeOrdersTable
                .select(CoffeeOrdersTable.id)
                .where {
                    (CoffeeOrdersTable.userId eq userId) and (CoffeeOrdersTable.finishedTime eq null)
                }
                .limit(1)
                .any()

            if (hasActiveOrder) {
                throw DucksBadRequestError("Сначала завершите или отмените текущий заказ")
            }

            val requestedAt = Clock.System.now().toEpochMilliseconds()

            val updatedRows = UserTable
                .update(
                    where = {
                        (UserTable.id eq userId) and (UserTable.deletionRequestedAt eq null)
                    }
                ) {
                    // Номер освобождаем сразу, иначе уникальный индекс не дал бы клиенту
                    // зарегистрироваться заново до конца отсрочки.
                    it[deletedPhoneNumber] = user[UserTable.phoneNumber]
                    it[phoneNumber] = deletedClientPhoneNumber(userId)
                    it[deletionRequestedAt] = requestedAt
                    it[fcmToken] = null
                }

            if (updatedRows == 0) {
                throw DucksBadRequestError("Аккаунт уже удалён")
            }

            // Подтверждение одноразовое. Гасим только после успешной записи: на отказах
            // вроде незавершённого заказа код останется годным, и клиент не запрашивает новый.
            confirmations.invalidate(userId)

            AccountDeletionDTO(
                requestedAt = requestedAt,
                dataRemovalAt = requestedAt + ACCOUNT_DELETION_GRACE_PERIOD.inWholeMilliseconds,
            )
        }
    }

    /**
     * Удаляет анонимный аккаунт — тот, у которого нет номера телефона. Подтверждать нечем:
     * СМС отправить некуда, и единственное доказательство владения аккаунтом — сам токен,
     * по которому пришёл запрос. Потому здесь нет [DELETION_CONFIRMATION_HEADER].
     *
     * Отсрочки [ACCOUNT_DELETION_GRACE_PERIOD] тоже нет: она нужна была, чтобы продавец мог
     * опознать клиента по номеру в заказах последних дней, а у анонимного аккаунта номера
     * нет и не было. Стирать нечего, поэтому строка обезличивается сразу — [deletedAt]
     * проставляется тем же временем, и [com.ducks.features.user.service.AnonymizeDeletedUsersService]
     * её уже не трогает. Саму строку не удаляем: на неё ссылаются заказы, из которых
     * считается выручка кофеен.
     *
     * Вернётся вход по номеру телефона — этот метод останется только для аккаунтов без
     * номера: у остальных удаление по-прежнему пойдёт через [requestDeletion] с кодом из СМС.
     */
    suspend fun requestAnonymousDeletion(userId: Long): AccountDeletionDTO {
        return newSuspendedTransaction {
            val user = UserTable
                .select(UserTable.phoneNumber, UserTable.deletionRequestedAt)
                .where { UserTable.id eq userId }
                .firstOrNull()
                ?: throw DucksBadRequestError("Аккаунт не найден")

            if (user[UserTable.deletionRequestedAt] != null) {
                throw DucksBadRequestError("Аккаунт уже удалён")
            }

            // Аккаунт с номером сюда попасть не должен: иначе удаление обошлось бы без кода
            // из СМС, которого требует requestDeletion.
            if (user[UserTable.phoneNumber] != null) {
                throw DucksBadRequestError("Удаление этого аккаунта подтверждается кодом из СМС")
            }

            // Заказ в работе оставлять нельзя: продавец останется с заказом от клиента,
            // которого в приложении уже нет.
            val hasActiveOrder = CoffeeOrdersTable
                .select(CoffeeOrdersTable.id)
                .where {
                    (CoffeeOrdersTable.userId eq userId) and (CoffeeOrdersTable.finishedTime eq null)
                }
                .any()

            if (hasActiveOrder) {
                throw DucksBadRequestError("Сначала завершите или отмените текущий заказ")
            }

            val requestedAt = Clock.System.now().toEpochMilliseconds()

            val updatedRows = UserTable
                .update(
                    where = {
                        (UserTable.id eq userId) and (UserTable.deletionRequestedAt eq null)
                    }
                ) {
                    it[deletionRequestedAt] = requestedAt
                    it[deletedAt] = requestedAt
                    it[fcmToken] = null
                    // Имя при анонимном входе не сохраняется, но у строки могло остаться
                    // прошлое: зануляем, чтобы «обезличена» значило обезличена.
                    it[name] = null
                    it[secondName] = null
                }

            // Между select и update успела пройти другая заявка на удаление.
            if (updatedRows == 0) {
                throw DucksBadRequestError("Аккаунт уже удалён")
            }

            AccountDeletionDTO(requestedAt = requestedAt, dataRemovalAt = requestedAt)
        }
    }
}

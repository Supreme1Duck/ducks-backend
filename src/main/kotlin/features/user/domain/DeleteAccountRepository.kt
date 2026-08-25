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
}

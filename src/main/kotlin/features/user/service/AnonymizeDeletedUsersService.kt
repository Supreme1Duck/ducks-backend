package com.ducks.features.user.service

import com.ducks.features.user.database.UserTable
import com.ducks.features.user.domain.ACCOUNT_DELETION_GRACE_PERIOD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Duration.Companion.hours

/**
 * Добивает удаление аккаунтов, у которых истекла отсрочка: стирает остатки персональных
 * данных, оставляя саму строку — на неё ссылаются заказы, из которых считается выручка
 * кофеен. Номер к этому моменту уже освобождён, это делается в момент заявки.
 */
class AnonymizeDeletedUsersService {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun invoke() {
        scope.launch {
            while (true) {
                anonymizeExpiredAccounts()

                // Отсрочка измеряется в днях, так что точность до часа тут с запасом.
                delay(1.hours)
            }
        }
    }

    private suspend fun anonymizeExpiredAccounts() {
        newSuspendedTransaction {
            val now = Clock.System.now().toEpochMilliseconds()
            val deadline = now - ACCOUNT_DELETION_GRACE_PERIOD.inWholeMilliseconds

            // У живых аккаунтов deletion_requested_at пуст, а NULL под lessEq не проходит,
            // поэтому отдельная проверка на «заявка вообще была» не нужна.
            UserTable.update(
                where = {
                    (UserTable.deletionRequestedAt lessEq deadline) and (UserTable.deletedAt eq null)
                }
            ) {
                it[name] = null
                it[secondName] = null
                it[fcmToken] = null
                it[deletedPhoneNumber] = null
                it[deletedAt] = now
            }
        }
    }
}

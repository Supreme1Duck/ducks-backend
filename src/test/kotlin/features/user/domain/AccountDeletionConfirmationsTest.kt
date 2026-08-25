package features.user.domain

import com.ducks.features.user.domain.AccountDeletionConfirmations
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountDeletionConfirmationsTest {

    private val confirmations = AccountDeletionConfirmations()

    private val userId = 1L
    private val otherUserId = 2L

    @Test
    fun `выданный токен подтверждает удаление`() {
        val issued = confirmations.issue(userId)

        assertTrue(confirmations.isConfirmed(userId, issued.confirmationToken))
    }

    @Test
    fun `чужой токен не подходит`() {
        val issued = confirmations.issue(userId)

        assertFalse(confirmations.isConfirmed(otherUserId, issued.confirmationToken))
    }

    @Test
    fun `случайный токен не подходит`() {
        confirmations.issue(userId)

        assertFalse(confirmations.isConfirmed(userId, "не тот токен"))
    }

    @Test
    fun `без выданного подтверждения удаление не проходит`() {
        assertFalse(confirmations.isConfirmed(userId, "любой токен"))
    }

    @Test
    fun `погашенный токен второй раз не срабатывает`() {
        val issued = confirmations.issue(userId)
        confirmations.invalidate(userId)

        assertFalse(confirmations.isConfirmed(userId, issued.confirmationToken))
    }

    @Test
    fun `повторная выдача обесценивает предыдущий токен`() {
        val first = confirmations.issue(userId)
        val second = confirmations.issue(userId)

        assertNotEquals(first.confirmationToken, second.confirmationToken)
        assertFalse(confirmations.isConfirmed(userId, first.confirmationToken))
        assertTrue(confirmations.isConfirmed(userId, second.confirmationToken))
    }

    @Test
    fun `попытки блокируются после пяти неверных кодов`() {
        repeat(4) { confirmations.recordFailedAttempt(userId) }
        assertNull(confirmations.secondsUntilAttemptsReset(userId), "четырёх попыток мало для блокировки")

        confirmations.recordFailedAttempt(userId)

        val wait = confirmations.secondsUntilAttemptsReset(userId)
        assertNotNull(wait, "после пятой попытки проверка кода должна блокироваться")
        assertTrue(wait in 1..15 * 60, "ожидание должно укладываться в окно блокировки, а не $wait")
    }

    @Test
    fun `неудачи одного пользователя не блокируют другого`() {
        repeat(5) { confirmations.recordFailedAttempt(userId) }

        assertNull(confirmations.secondsUntilAttemptsReset(otherUserId))
    }

    @Test
    fun `верный код сбрасывает счётчик неудач`() {
        repeat(5) { confirmations.recordFailedAttempt(userId) }
        confirmations.issue(userId)

        assertNull(confirmations.secondsUntilAttemptsReset(userId))
    }

    @Test
    fun `срок жизни подтверждения отдаётся клиенту`() {
        val before = System.currentTimeMillis()

        val issued = confirmations.issue(userId)

        val ttl = issued.expiresAt - before

        assertTrue(
            ttl in (4 * 60 * 1000L)..(5 * 60 * 1000L),
            "подтверждение должно жить около пяти минут, а не $ttl мс",
        )
    }
}

package com.ducks.features.user.domain

import com.ducks.features.user.data.dto.AccountDeletionConfirmationDTO
import kotlinx.datetime.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Одноразовые подтверждения удаления аккаунта: клиент вводит код из СМС, получает токен
 * и предъявляет его при самом удалении. Без этого проверка кода была бы декоративной —
 * удалить аккаунт можно было бы одним лишь JWT, минуя экран с кодом.
 *
 * Хранение в памяти, как и у [com.ducks.features.user.ratelimit.OtpRateLimiter]: токен
 * живёт минуты, и потерять его при рестарте не страшно — клиент просто запросит код заново.
 */
class AccountDeletionConfirmations {

    private data class Confirmation(
        val token: String,
        val expiresAt: Long,
    )

    private data class FailedAttempts(
        val count: Int,
        val firstAttemptAt: Long,
    )

    private val confirmations = ConcurrentHashMap<Long, Confirmation>()
    private val failedAttempts = ConcurrentHashMap<Long, FailedAttempts>()

    companion object {
        private val CONFIRMATION_TTL = 5.minutes

        // Код шестизначный, так что перебор без ограничения попыток вполне реален.
        private const val MAX_FAILED_ATTEMPTS = 5
        private val FAILED_ATTEMPTS_WINDOW = 15.minutes
    }

    /**
     * Сколько секунд ждать, если попытки ввода кода исчерпаны. null — можно проверять.
     */
    fun secondsUntilAttemptsReset(userId: Long): Long? {
        val attempts = failedAttempts[userId] ?: return null

        val elapsed = Clock.System.now().toEpochMilliseconds() - attempts.firstAttemptAt
        if (elapsed >= FAILED_ATTEMPTS_WINDOW.inWholeMilliseconds) {
            failedAttempts.remove(userId)
            return null
        }

        if (attempts.count < MAX_FAILED_ATTEMPTS) return null

        return (FAILED_ATTEMPTS_WINDOW.inWholeMilliseconds - elapsed + 999) / 1000
    }

    fun recordFailedAttempt(userId: Long) {
        val now = Clock.System.now().toEpochMilliseconds()

        failedAttempts.compute(userId) { _, existing ->
            val windowExpired = existing != null &&
                    now - existing.firstAttemptAt >= FAILED_ATTEMPTS_WINDOW.inWholeMilliseconds

            if (existing == null || windowExpired) {
                FailedAttempts(count = 1, firstAttemptAt = now)
            } else {
                existing.copy(count = existing.count + 1)
            }
        }
    }

    /**
     * Выдаёт подтверждение после верного кода. Предыдущее для этого пользователя перестаёт
     * действовать: одновременно живым должно быть не больше одного токена.
     */
    fun issue(userId: Long): AccountDeletionConfirmationDTO {
        val expiresAt = Clock.System.now().toEpochMilliseconds() + CONFIRMATION_TTL.inWholeMilliseconds
        val token = UUID.randomUUID().toString()

        confirmations[userId] = Confirmation(token = token, expiresAt = expiresAt)
        failedAttempts.remove(userId)

        return AccountDeletionConfirmationDTO(
            confirmationToken = token,
            expiresAt = expiresAt,
        )
    }

    fun isConfirmed(userId: Long, token: String): Boolean {
        val confirmation = confirmations[userId] ?: return false

        if (confirmation.expiresAt <= Clock.System.now().toEpochMilliseconds()) {
            confirmations.remove(userId)
            return false
        }

        return confirmation.token == token
    }

    fun invalidate(userId: Long) {
        confirmations.remove(userId)
        failedAttempts.remove(userId)
    }
}

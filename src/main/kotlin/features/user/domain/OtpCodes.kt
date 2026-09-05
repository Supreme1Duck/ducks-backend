package com.ducks.features.user.domain

import kotlinx.datetime.Clock
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

class OtpCodes {

    private data class Code(
        val value: String,
        val expiresAt: Long,
        val attemptsLeft: Int,
    )

    private val codes = ConcurrentHashMap<String, Code>()

    private val random = SecureRandom()

    companion object {
        private val CODE_TTL = 5.minutes

        const val CODE_LENGTH = 6

        // Шесть цифр перебираются за миллион попыток, поэтому попытки ограничены:
        // после промахов код сгорает и нужно запрашивать новый.
        private const val MAX_ATTEMPTS = 5
    }

    /** Новый случайный код. Хранить его — отдельным шагом, см. [remember]. */
    fun generate(): String = (1..CODE_LENGTH)
        .map { random.nextInt(10) }
        .joinToString("")

    /**
     * Запоминает код за номером. Предыдущий код этого номера перестаёт действовать:
     * иначе после повторного запроса подошли бы оба, и ограничение попыток удвоилось бы.
     */
    fun remember(phoneNumber: String, code: String) {
        codes[phoneNumber] = Code(
            value = code,
            expiresAt = Clock.System.now().toEpochMilliseconds() + CODE_TTL.inWholeMilliseconds,
            attemptsLeft = MAX_ATTEMPTS,
        )
    }

    /**
     * Проверка кода. Верный код сгорает — повторно тем же кодом ни войти, ни удалить
     * аккаунт нельзя. Неверный тратит попытку, после последней код тоже сгорает.
     */
    fun verify(phoneNumber: String, otp: String): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        var verified = false

        // compute, а не чтение с последующей записью: один и тот же код могут проверять
        // одновременно, и без атомарности попытки списывались бы мимо.
        codes.compute(phoneNumber) { _, code ->
            when {
                code == null -> null
                code.expiresAt <= now -> null
                code.value == otp -> {
                    verified = true
                    null
                }
                // Последняя попытка израсходована — код сгорает вместе с ней.
                code.attemptsLeft <= 1 -> null
                else -> code.copy(attemptsLeft = code.attemptsLeft - 1)
            }
        }

        return verified
    }
}

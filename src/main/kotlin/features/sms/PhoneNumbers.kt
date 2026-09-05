package com.ducks.features.sms

/**
 * Приведение номера к виду, который принимает sms.by: только цифры, в международном
 * формате и без плюса (375291234567).
 *
 * Клиент присылает номер так, как его набрал пользователь: с плюсом, скобками и
 * пробелами, иногда в местной записи (8 029 …) или вовсе без кода страны. В базе номер
 * так и хранится, поэтому приводим его только на границе с оператором — иначе один и
 * тот же человек стал бы разными пользователями.
 */
object PhoneNumbers {

    private const val BELARUS_CODE = "375"

    /** Столько цифр в белорусском номере без кода страны и без ведущего нуля: 29 123 45 67. */
    private const val BELARUS_LOCAL_LENGTH = 9

    // Границы по E.164: телефонов короче 8 цифр не бывает, длиннее 15 не бывает вовсе.
    private val VALID_LENGTH = 8..15

    /** @return номер для sms.by или null, если на номер это не похоже. */
    fun toOperatorFormat(rawPhoneNumber: String): String? {
        val digits = rawPhoneNumber.filter { it.isDigit() }

        val international = when {
            // Местная запись: 8 029 123-45-67.
            digits.length == BELARUS_LOCAL_LENGTH + 2 && digits.startsWith("80") ->
                BELARUS_CODE + digits.drop(2)

            // Она же без восьмёрки: 029 123-45-67.
            digits.length == BELARUS_LOCAL_LENGTH + 1 && digits.startsWith("0") ->
                BELARUS_CODE + digits.drop(1)

            // Номер без кода страны — считаем белорусским, других стран у нас пока нет.
            digits.length == BELARUS_LOCAL_LENGTH -> BELARUS_CODE + digits

            else -> digits
        }

        return international.takeIf { it.length in VALID_LENGTH }
    }
}

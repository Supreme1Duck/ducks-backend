package com.ducks.features.user.domain

import com.ducks.features.sms.SmsSender

class OtpService(
    private val codes: OtpCodes,
    private val smsSender: SmsSender,
) {

    suspend fun sendCode(phoneNumber: String): Boolean {
        val code = codes.generate()

        if (!smsSender.send(phoneNumber, message(code))) return false

        codes.remember(phoneNumber, code)

        return true
    }

    fun verify(phoneNumber: String, otp: String): Boolean = codes.verify(phoneNumber, otp)

    // Кириллица режет SMS на части по 70 символов, поэтому текст короткий: одна часть.
    private fun message(code: String) = "Код для входа в Утки: $code. Никому не сообщайте его."
}

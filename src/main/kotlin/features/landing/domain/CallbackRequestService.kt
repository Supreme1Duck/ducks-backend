package com.ducks.features.landing.domain

import com.ducks.features.sms.PhoneNumbers
import com.ducks.features.telegram.TelegramNotifier
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Заявка на обратный звонок с лендинга: кофейня оставляет номер, мы получаем
 * уведомление в свой чат и перезваниваем. Заявки нигде не хранятся — их мало,
 * и живут они ровно до звонка, так что база тут была бы лишней сущностью.
 */
class CallbackRequestService(
    private val notifier: TelegramNotifier,
) {

    companion object {
        // UTC+3 круглый год, как и в остальных расчётах времени по кофейням.
        private val TIME_ZONE: ZoneId = ZoneId.of("Europe/Minsk")

        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    /**
     * @return номер в международном формате без плюса или null, если на телефон
     * это не похоже. Формат тот же, что для оператора: номер набирают руками,
     * и приводить его к одному виду нужно и здесь — иначе лимит по номеру
     * обойдётся лишним пробелом.
     */
    fun normalize(rawPhoneNumber: String): String? = PhoneNumbers.toOperatorFormat(rawPhoneNumber)

    /** @return true, если уведомление ушло в чат. */
    suspend fun send(phoneNumber: String): Boolean = notifier.notify(message(phoneNumber))

    private fun message(phoneNumber: String): String {
        val time = TIME_FORMAT.format(Instant.now().atZone(TIME_ZONE))

        return """
            🦆 Заявка с лендинга
            
            Телефон: +$phoneNumber
            Время: $time
        """.trimIndent()
    }
}

package com.ducks.features.sms

/**
 * Отправка одиночного SMS. Реализации: [SmsBySender] — боевая, [LoggingSmsSender] — для
 * локальной разработки.
 */
interface SmsSender {

    /**
     * @return true, если оператор принял сообщение. Доставку это не гарантирует:
     * sms.by отвечает статусом NEW, а дальше судьбу сообщения знает только checkSMS.
     */
    suspend fun send(phoneNumber: String, text: String): Boolean
}

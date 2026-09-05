package com.ducks.features.sms

import org.slf4j.LoggerFactory

/**
 * Заглушка на время разработки: вместо отправки пишет сообщение в лог. Включается сама,
 * когда не задан токен sms.by, — код при этом настоящий и проверяется как обычно,
 * поэтому вход работает без денег на балансе и без ключа.
 */
class LoggingSmsSender : SmsSender {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun send(phoneNumber: String, text: String): Boolean {
        logger.warn("SMS не настроена (нет sms.token), сообщение на {} не отправлено: {}", phoneNumber, text)
        return true
    }
}

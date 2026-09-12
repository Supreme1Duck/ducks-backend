package com.ducks.features.telegram

import org.slf4j.LoggerFactory

/**
 * Заглушка на время разработки: вместо отправки пишет сообщение в лог. Включается сама,
 * когда не задан токен бота, — чтобы поднять сервер локально не нужно заводить бота
 * и знать id чата.
 */
class LoggingTelegramNotifier : TelegramNotifier {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun notify(text: String): Boolean {
        logger.warn("Telegram не настроен (нет telegram.botToken), уведомление не отправлено: {}", text)
        return true
    }
}

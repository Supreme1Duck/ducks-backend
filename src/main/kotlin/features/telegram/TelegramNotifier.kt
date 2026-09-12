package com.ducks.features.telegram

/**
 * Уведомление в наш собственный чат: сюда падают события, за которыми следит человек,
 * а не приложение. Реализации: [TelegramBotNotifier] — боевая, [LoggingTelegramNotifier] —
 * для локальной разработки.
 */
interface TelegramNotifier {

    /** @return true, если Telegram принял сообщение. */
    suspend fun notify(text: String): Boolean
}

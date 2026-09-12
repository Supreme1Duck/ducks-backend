package com.ducks.features.telegram

import org.koin.dsl.module

fun telegramModule(config: TelegramConfig) = module {

    // Без токена или chat_id поднимаемся с заглушкой: локальной разработке бот не нужен.
    single<TelegramNotifier> {
        if (config.botToken.isBlank() || config.chatId.isBlank()) {
            LoggingTelegramNotifier()
        } else {
            TelegramBotNotifier(get(), config)
        }
    }
}

package com.ducks.features.sms

import org.koin.dsl.module

fun smsModule(config: SmsByConfig) = module {

    // Без токена поднимаемся с заглушкой: локальной разработке ключ оператора не нужен.
    single<SmsSender> {
        if (config.token.isBlank()) LoggingSmsSender() else SmsBySender(get(), config)
    }
}

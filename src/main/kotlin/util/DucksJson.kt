package com.ducks.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import util.BigDecimalSerializer
import java.math.BigDecimal

/**
 * Правила разбора json для всего приложения.
 *
 * Вынесены из ContentNegotiation, потому что тела запросов разбирает не только он:
 * в multipart json приезжает обычным полем формы и парсится руками. С отдельным
 * `Json` в таком месте одна и та же модель повела бы себя по-разному в зависимости
 * от того, каким запросом приехала.
 */
val ducksJson = Json {
    serializersModule = SerializersModule {
        contextual(BigDecimal::class, BigDecimalSerializer)
    }
}

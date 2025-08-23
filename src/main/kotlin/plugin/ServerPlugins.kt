package com.ducks.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import util.BigDecimalSerializer
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds

fun Application.installServerPlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = SerializersModule {
                    contextual(BigDecimal::class, BigDecimalSerializer)
                }
            }
        )
    }

    install(WebSockets) {
        pingPeriod = 5.times(1000).milliseconds
        timeout = 30.times(1000).milliseconds
    }
}
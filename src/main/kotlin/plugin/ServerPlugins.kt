package com.ducks.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import com.ducks.util.ducksJson
import kotlin.time.Duration.Companion.milliseconds

fun Application.installServerPlugins() {
    install(ContentNegotiation) {
        json(ducksJson)
    }

    install(WebSockets) {
        pingPeriod = 5.times(1000).milliseconds
        timeout = 30.times(1000).milliseconds
    }
}
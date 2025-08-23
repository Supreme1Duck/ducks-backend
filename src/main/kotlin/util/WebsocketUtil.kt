package com.ducks.util

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Route.ducksWebSocket(
    route: String,
    block: suspend DefaultWebSocketServerSession.() -> Unit
) {
    webSocket(route) {
        try {
            val heartbeatJob = launch {
                heartbeat()
            }

            launch {
                block()
            }

            heartbeatJob.join()
        } catch (e: Exception) {
            close(
                CloseReason(CloseReason.Codes.GOING_AWAY, e.localizedMessage)
            )
        }
    }
}

private suspend fun DefaultWebSocketServerSession.heartbeat() {
    while (true) {
        delay(10000)
        try {
            send(Frame.Ping(ByteArray(0)))
        } catch (e: Throwable) {
            println("Не удалось отправить ping - клиент отключился: ${e.message}")
            // По умолчанию выбрасывается Cancellation
            throw IllegalArgumentException()
        }
    }
}
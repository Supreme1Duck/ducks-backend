package com.ducks.features.telegram

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Отправка через Bot API (https://core.telegram.org/bots/api#sendmessage).
 *
 * Параметры уходят в query, а не телом: у ktor-клиента приложения не установлен
 * ContentNegotiation, а форма запроса для одного метода того не стоит.
 *
 * parse_mode намеренно не передаём: текст собирается из пользовательского ввода,
 * и без разметки его не нужно экранировать — сообщение уходит как есть.
 */
class TelegramBotNotifier(
    private val ktor: HttpClient,
    private val config: TelegramConfig,
) : TelegramNotifier {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SendMessageResponse(
        val ok: Boolean = false,
        val description: String? = null,
    )

    companion object {
        private const val API_URL = "https://api.telegram.org"

        // Уведомления ждёт наш чат, а не человек на экране, но запрос всё равно
        // висит внутри обработки формы — держать его до упора незачем.
        private const val CONNECT_TIMEOUT_MILLIS = 5_000L
        private const val REQUEST_TIMEOUT_MILLIS = 10_000L
    }

    override suspend fun notify(text: String): Boolean {
        val (status, body) = try {
            val response = ktor.post("$API_URL/bot${config.botToken}/sendMessage") {
                timeout {
                    connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }

                parameter("chat_id", config.chatId)
                parameter("text", text)
                parameter("disable_web_page_preview", true)
            }

            // Тело читаем здесь же: таймаут срабатывает и на чтении ответа.
            response.status to response.bodyAsText()
        } catch (e: Exception) {
            logger.error("Telegram недоступен при отправке уведомления", e)
            return false
        }

        val parsed = try {
            json.decodeFromString<SendMessageResponse>(body)
        } catch (e: Exception) {
            logger.error("Telegram ответил неразборчивым телом ($status): $body", e)
            return false
        }

        // На ошибках Bot API отвечает не 200 и кладёт причину в description.
        if (!status.isSuccess() || !parsed.ok) {
            logger.error("Telegram не принял уведомление ({}): {}", status, parsed.description ?: body)
            return false
        }

        return true
    }
}

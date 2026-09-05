package com.ducks.features.sms

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Отправка через sms.by (https://app.sms.by/api/user/api-docs).
 *
 * Берём одиночную отправку sendQuickSMS, а не 2FA-методы того же API
 * (createPasswordObject + sendSmsMessageWithCode): там код генерирует и хранит оператор,
 * а нам код нужен свой — тот же самый проверяется и при входе, и при удалении аккаунта,
 * и переживать поход в чужой сервис ради сравнения строк незачем.
 *
 * Токен и параметры уходят в query — так описано в документации, тела у запроса нет.
 */
class SmsBySender(
    private val ktor: HttpClient,
    private val config: SmsByConfig,
) : SmsSender {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Ответ приходит с разным набором полей: у ошибок нет sms_id, у успеха нет error.
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SendSmsResponse(
        @SerialName("sms_id") val smsId: Long? = null,
        val status: String? = null,
        val error: String? = null,
    )

    companion object {
        private const val SEND_URL = "https://app.sms.by/api/v1/sendQuickSMS"

        // Отправки ждёт живой человек на экране ввода номера, поэтому предел жёсткий:
        // лучше сказать "не удалось, попробуйте ещё раз", чем держать запрос до упора.
        // Повторов нет намеренно — за повтор отвечает сам клиент, и каждая лишняя
        // попытка это ещё одно сообщение и ещё одно списание с баланса.
        private const val CONNECT_TIMEOUT_MILLIS = 5_000L
        private const val REQUEST_TIMEOUT_MILLIS = 10_000L
    }

    override suspend fun send(phoneNumber: String, text: String): Boolean {
        val phone = PhoneNumbers.toOperatorFormat(phoneNumber)

        if (phone == null) {
            logger.warn("Номер {} не похож на телефон, отправка отменена", phoneNumber)
            return false
        }

        val (status, body) = try {
            val response = ktor.post(SEND_URL) {
                timeout {
                    connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }

                parameter("token", config.token)
                parameter("phone", phone)
                parameter("message", text)
                parameter("alphaname_id", config.alphanameId)
            }

            // Тело читаем здесь же: таймаут срабатывает и на чтении ответа,
            // а ловить его снаружи было бы уже некому.
            response.status to response.bodyAsText()
        } catch (e: Exception) {
            // Ни недоступность оператора, ни таймаут не должны ронять запрос клиента:
            // ему ответим, что код не ушёл, и он сможет попробовать снова.
            logger.error("sms.by недоступен при отправке на $phone", e)
            return false
        }

        if (!status.isSuccess()) {
            logger.error("sms.by вернул {} на отправку на {}: {}", status, phone, body)
            return false
        }

        val parsed = try {
            json.decodeFromString<SendSmsResponse>(body)
        } catch (e: Exception) {
            logger.error("sms.by ответил неразборчивым телом на отправку на $phone: $body", e)
            return false
        }

        // Оператор умеет отвечать 200 с текстом ошибки вместо идентификатора.
        if (parsed.smsId == null) {
            logger.error("sms.by не принял сообщение на {}: {}", phone, parsed.error ?: body)
            return false
        }

        logger.info("SMS {} на {} принято, статус {}", parsed.smsId, phone, parsed.status)

        return true
    }
}

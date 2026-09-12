package com.ducks.features.landing.route

import com.ducks.features.landing.domain.CallbackRequestService
import com.ducks.features.landing.ratelimit.CallbackRateLimiter
import com.ducks.features.telegram.TelegramNotifier
import com.ducks.util.ducksJson
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallbackRequestRouteTest {

    private class RecordingNotifier(private val delivers: Boolean = true) : TelegramNotifier {
        val messages = mutableListOf<String>()

        override suspend fun notify(text: String): Boolean {
            messages += text
            return delivers
        }
    }

    private fun ApplicationTestBuilder.landingApplication(notifier: TelegramNotifier) {
        application {
            install(ContentNegotiation) { json(ducksJson) }
            install(Koin) {
                modules(
                    module {
                        single<TelegramNotifier> { notifier }
                        single { CallbackRateLimiter() }
                        single { CallbackRequestService(get()) }
                    }
                )
            }
            routing { landingRoute() }
        }
    }

    private suspend fun ApplicationTestBuilder.submit(phoneNumber: String) =
        client.post("/landing/callback-requests") {
            contentType(ContentType.Application.Json)
            setBody("""{"phoneNumber":"$phoneNumber"}""")
        }

    @Test
    fun `заявка уходит уведомлением в чат`() = testApplication {
        val notifier = RecordingNotifier()
        landingApplication(notifier)

        val response = submit("+375 (29) 123-45-67")

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(1, notifier.messages.size)
        assertTrue(notifier.messages.single().contains("+375291234567"))
    }

    @Test
    fun `номер не похожий на телефон отклоняется`() = testApplication {
        val notifier = RecordingNotifier()
        landingApplication(notifier)

        val response = submit("не телефон")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(notifier.messages.isEmpty())
        // Текст показывается прямо в форме, поэтому он без json-обёртки.
        assertEquals("Проверьте номер телефона.", response.bodyAsText())
    }

    @Test
    fun `повторная заявка с того же номера не дублируется`() = testApplication {
        val notifier = RecordingNotifier()
        landingApplication(notifier)

        assertEquals(HttpStatusCode.Created, submit("375291234567").status)

        // Тот же номер, записанный иначе, — та же кофейня.
        val repeat = submit("8 029 123-45-67")

        assertEquals(HttpStatusCode.TooManyRequests, repeat.status)
        assertEquals(1, notifier.messages.size)
    }

    @Test
    fun `недоступный telegram не выдаёт заявку за принятую`() = testApplication {
        val notifier = RecordingNotifier(delivers = false)
        landingApplication(notifier)

        assertEquals(HttpStatusCode.BadGateway, submit("375291234567").status)
    }
}

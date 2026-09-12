package com.ducks.features.landing.route

import com.ducks.features.landing.domain.CallbackRequestService
import com.ducks.features.landing.ratelimit.CallbackRateLimiter
import com.ducks.features.landing.route.request.CallbackRequest
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

// Промо-страница проекта: отдаётся с корня домена, публично и без авторизации.
// Вёрстка и картинки лежат в src/main/resources/landing, поэтому едут вместе с jar
// и отдельного деплоя лендингу не нужно.
fun Route.landingRoute() {
    callbackRequestRoute()

    staticResources(
        remotePath = "/",
        basePackage = "landing",
        index = "index.html"
    )
}

/**
 * Форма «перезвоните мне» с лендинга. Ручка публичная — её дёргает страница из браузера,
 * поэтому единственная защита от мусора это лимиты по ip и номеру.
 *
 * Ошибки отдаём plain text: страница показывает тело ответа как есть, а
 * ContentNegotiation завернул бы строку в json вместе с кавычками.
 */
private fun Route.callbackRequestRoute() {
    val callbackRequests by application.inject<CallbackRequestService>()
    val rateLimiter by application.inject<CallbackRateLimiter>()

    post("/landing/callback-requests") {
        val request = try {
            call.receive<CallbackRequest>()
        } catch (e: Exception) {
            return@post call.respondText(
                "Не удалось разобрать запрос.",
                status = HttpStatusCode.BadRequest,
            )
        }

        val phoneNumber = callbackRequests.normalize(request.phoneNumber)
            ?: return@post call.respondText(
                "Проверьте номер телефона.",
                status = HttpStatusCode.BadRequest,
            )

        // За прокси адрес клиента приезжает заголовком, напрямую — из соединения.
        val ip = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
            ?: call.request.local.remoteAddress

        val wait = rateLimiter.checkIpLimit(ip) ?: rateLimiter.checkPhoneLimit(phoneNumber)

        if (wait != null) {
            call.response.headers.append(HttpHeaders.RetryAfter, wait.toString())
            return@post call.respondText(
                "Заявка уже принята. Мы скоро позвоним.",
                status = HttpStatusCode.TooManyRequests,
            )
        }

        // Считаем попытку до отправки: иначе недоступный Telegram превращает ручку
        // в бесплатный способ долбить нас запросами.
        rateLimiter.recordIpRequest(ip)

        if (!callbackRequests.send(phoneNumber)) {
            return@post call.respondText(
                "Не удалось отправить заявку. Напишите нам в Telegram.",
                status = HttpStatusCode.BadGateway,
            )
        }

        rateLimiter.recordPhoneRequest(phoneNumber)

        call.respond(HttpStatusCode.Created)
    }
}

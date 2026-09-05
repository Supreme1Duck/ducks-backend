package com.ducks.features.config.route

import com.ducks.features.config.domain.ClientConfigRepository
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Что доступно в приложении. Публичная, без авторизации: приложение спрашивает конфиг
 * на старте, до логина, и должно уметь спрятать выключенную фичу ещё на первом экране.
 */
fun Route.clientConfigRoute() {

    val configRepository by application.inject<ClientConfigRepository>()

    get("/config") {
        ducksTryCatch {
            // Смысл конфига в том, чтобы выключение доезжало сразу: закэшированный
            // на устройстве ответ обнулил бы рубильник.
            call.response.headers.append(HttpHeaders.CacheControl, "no-store")

            call.respond(HttpStatusCode.OK, configRepository.getConfig())
        }
    }
}

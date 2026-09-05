package com.ducks.admin.route

import com.ducks.admin.request.UpdateClientFeatureRequest
import com.ducks.features.config.domain.ClientConfigRepository
import com.ducks.features.config.model.ClientFeature
import com.ducks.util.DucksBadRequestError
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Рубильник фич клиентского приложения. Отдельная ручка, а не переменная окружения:
 * выключать фичу приходится тогда, когда она уже сломалась у людей, и ждать
 * перезапуск сервера в этот момент — самое дорогое, что можно сделать.
 */
fun Route.adminClientConfigRoute() {

    val configRepository by application.inject<ClientConfigRepository>()

    route("/client-config") {

        // Ровно то же, что видит приложение: и дефолты, и переключённые флаги.
        get {
            ducksTryCatch {
                call.respond(HttpStatusCode.OK, configRepository.getConfig())
            }
        }

        put("/{key}") {
            ducksTryCatch {
                val key = call.parameters["key"].orEmpty()

                val feature = ClientFeature.ofKey(key)
                    ?: throw DucksBadRequestError(
                        "Неизвестный флаг: $key. Доступны: ${ClientFeature.entries.joinToString { it.key }}"
                    )

                val request = call.receive<UpdateClientFeatureRequest>()

                val config = configRepository.setEnabled(feature, request.isEnabled)

                call.respond(HttpStatusCode.OK, config)
            }
        }
    }
}

package com.ducks.admin.route

import com.ducks.admin.repository.AdminCoffeeShopsRepository
import com.ducks.features.coffeeshops.service.ProductRecommendationsService
import com.ducks.util.DucksBadRequestError
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Ручной пересчёт статистики совместных покупок. Сам по себе он идёт ночью, но ждать
 * до утра приходится в двух случаях: сразу после выкатки, когда таблица ещё пустая,
 * и после правки порогов подбора, когда результат хочется увидеть на живых данных.
 */
fun Route.adminRecommendationsRoute() {
    val recommendationsService by application.inject<ProductRecommendationsService>()
    val coffeeShopsRepository by application.inject<AdminCoffeeShopsRepository>()

    route("/coffee-shops/recommendations/recalculate") {

        // Все кофейни разом. Ответ ждём: пересчёт укладывается в секунды,
        // а знать, сколько пар получилось, важнее, чем быстро отпустить запрос.
        post {
            ducksTryCatch {
                val report = recommendationsService.recalculateNow()

                call.respond(HttpStatusCode.OK, report)
            }
        }

        // Одна кофейня: обычный случай при отладке порогов на конкретной точке.
        post("/{shopId}") {
            ducksTryCatch {
                val shopId = call.parameters["shopId"]?.toLongOrNull()
                    ?: throw DucksBadRequestError("Некорректный id кофейни")

                coffeeShopsRepository.ensureShopExists(shopId)

                val report = recommendationsService.recalculateNow(shopId)

                call.respond(HttpStatusCode.OK, report)
            }
        }
    }
}

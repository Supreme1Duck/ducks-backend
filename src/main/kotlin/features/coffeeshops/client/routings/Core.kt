package com.ducks.features.coffeeshops.client.routings

import com.ducks.common.geo.GeoPoint
import com.ducks.common.geo.toDegreesOrThrow
import com.ducks.features.coffeeshops.client.domain.CoffeeProductsRepository
import com.ducks.features.coffeeshops.client.domain.CoffeeShopsRepository
import com.ducks.features.coffeeshops.client.data.model.dto.CheckProductsExistenceResponse
import com.ducks.features.coffeeshops.client.routings.request.CheckProductsExistenceRequest
import com.ducks.features.coffeeshops.client.routings.request.EstimateCookingTimeRequest
import com.ducks.features.coffeeshops.client.routings.request.OrderTimeRequest
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.clientRoute() {

    val coffeeShopsRepository by application.inject<CoffeeShopsRepository>()
    val coffeeProductsRepository by application.inject<CoffeeProductsRepository>()

    get("/list") {
        ducksTryCatch {
            val lastId = call.parameters["lastId"]?.toLong()
            val limit = call.parameters["limit"]?.toInt()
            val offset = call.parameters["offset"]?.toLong()

            // Геолокация клиента: если пришла — список сортируется по удалённости.
            val userLocation = GeoPoint.parse(
                latitude = call.parameters["lat"]?.toDegreesOrThrow("lat"),
                longitude = call.parameters["lon"]?.toDegreesOrThrow("lon"),
            )

            val shops = coffeeShopsRepository.getShopsList(
                lastId = lastId,
                limit = limit,
                offset = offset,
                userLocation = userLocation,
            )

            call.respond(HttpStatusCode.OK, shops)
        }
    }

    get("/shop/{shopId}") {
        ducksTryCatch {
            val shopId = call.parameters["shopId"]!!.toLong()

            val shop = coffeeShopsRepository.getShop(shopId)

            call.respond(HttpStatusCode.OK, shop)
        }
    }

    get("/product/{id}") {
        ducksTryCatch {
            val productId = call.parameters["productId"]!!.toLong()

            val product = coffeeProductsRepository.getProduct(productId)

            call.respond(HttpStatusCode.OK, product)
        }
    }

    post("/products/estimate-cooking-time") {
        ducksTryCatch {
            val request = call.receive<EstimateCookingTimeRequest>()
            val estimate = coffeeProductsRepository.estimateCookingTime(request)
            call.respond(HttpStatusCode.OK, estimate)
        }
    }

    // Проверка существования пар (shopId, productId): в ответе — только несуществующие пары.
    post("/products/check-existence") {
        ducksTryCatch {
            val request = call.receive<CheckProductsExistenceRequest>()

            val missing = coffeeProductsRepository.findMissingPairs(request.pairs)

            call.respond(HttpStatusCode.OK, CheckProductsExistenceResponse(missing = missing))
        }
    }

    post("/shop/order-time") {
        ducksTryCatch {
            val request = call.receive<OrderTimeRequest>()

            val availableTimesToOrder = coffeeShopsRepository.getOrdersTimeList(request.shopId, request.productIds)

            call.respond(availableTimesToOrder)
        }
    }
}
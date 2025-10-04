package com.ducks.features.coffeeshops.client.routings

import com.ducks.features.coffeeshops.client.domain.CoffeeProductsRepository
import com.ducks.features.coffeeshops.client.domain.CoffeeShopsRepository
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
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

            val shops = coffeeShopsRepository.getShopsList(lastId, limit)

            call.respond(HttpStatusCode.OK, shops)
        }
    }

    get("/shop/{id}") {
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

    get("/shop/order-time") {
        ducksTryCatch {
            val shopId = call.parameters["shopId"]!!.toLong()
            val estimatedOrderFinishTimeInMinutes = call.parameters["estimatedOrderFinishTimeInMinutes"]!!.toInt()

            val availableTimesToOrder = coffeeShopsRepository.getOrdersTimeList(shopId, estimatedOrderFinishTimeInMinutes)

            call.respond(availableTimesToOrder)
        }
    }
}
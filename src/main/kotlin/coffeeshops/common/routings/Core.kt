package com.ducks.coffeeshops.common.routings

import com.ducks.coffeeshops.common.domain.CoffeeProductsRepository
import com.ducks.coffeeshops.common.domain.CoffeeShopsRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.clientRoute() {

    val coffeeShopsRepository by application.inject<CoffeeShopsRepository>()
    val coffeeProductsRepository by application.inject<CoffeeProductsRepository>()

    get("/list") {
        try {
            val lastId = call.parameters["lastId"]?.toLong()
            val limit = call.parameters["limit"]?.toInt()

            val shops = coffeeShopsRepository.getShopsList(lastId, limit)

            call.respond(HttpStatusCode.OK, shops)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.message.toString())
        }
    }

    get("/shop/{id}") {
        try {
            val shopId = call.parameters["shopId"]!!.toLong()

            val shop = coffeeShopsRepository.getShop(shopId)

            call.respond(HttpStatusCode.OK, shop)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.message.toString())
        }
    }

    get("/product/{id}") {
        try {
            val productId = call.parameters["productId"]!!.toLong()

            val product = coffeeProductsRepository.getProduct(productId)

            call.respond(HttpStatusCode.OK, product)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.message.toString())
        }
    }
}
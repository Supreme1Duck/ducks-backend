package com.ducks.coffeeshops.seller.routings

import com.ducks.coffeeshops.seller.domain.SellerConstructorsRepository
import com.ducks.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.coffeeshops.seller.routings.request.constructor.CreateConstructorCategoryRequest
import com.ducks.coffeeshops.seller.routings.request.constructor.CreateConstructorRequest
import com.ducks.coffeeshops.seller.routings.request.constructor.DeleteConstructorRequest
import com.ducks.coffeeshops.seller.routings.request.constructor.SetInStockRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.constructorsRoute() {
    val constructorsRepository by application.inject<SellerConstructorsRepository>()

    route("/constructor") {
        get("/by-shop") {
            try {
                val shopId = getCoffeeShopSellerPrincipal().shopId

                val data = constructorsRepository.fetchByShop(shopId)

                call.respond(HttpStatusCode.OK, data)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/create/constructor") {
            try {
                val shopId = getCoffeeShopSellerPrincipal().shopId
                val request = call.receive<CreateConstructorRequest>()

                constructorsRepository.insertNewConstructor(
                    shopId = shopId,
                    request = request
                )

                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/create/category") {
            try {
                val shopId = getCoffeeShopSellerPrincipal().shopId
                val request = call.receive<CreateConstructorCategoryRequest>()

                constructorsRepository.insertNewCategory(
                    shopId = shopId,
                    request = request
                )

                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("update/stock") {
            try {
                val request = call.receive<SetInStockRequest>()

                constructorsRepository.setInStock(
                    setInStockRequest = request
                )

                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        delete("delete/constructor") {
            try {
                val request = call.receive<DeleteConstructorRequest>()

                constructorsRepository.deleteConstructor(
                    request = request
                )

                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        delete("delete/category") {
            try {
                val shopId = getCoffeeShopSellerPrincipal().shopId
                val request = call.receive<Long>()

                constructorsRepository.deleteCategory(
                    shopId = shopId,
                    categoryId = request,
                )

                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
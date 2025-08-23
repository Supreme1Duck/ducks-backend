package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.domain.ObserveOrdersRepository
import com.ducks.features.coffeeshops.seller.domain.OrdersRepository
import com.ducks.features.coffeeshops.seller.routings.request.orders.CancelBySellerRequest
import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.sellerOrdersRoute() {
    val orderRepository by application.inject<OrdersRepository>()
    val observeOrdersRepository by application.inject<ObserveOrdersRepository>()

    get("/check/orders") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId

            val currentOrders = observeOrdersRepository.getCurrentOrders(shopId = shopId)

            call.respond(HttpStatusCode.OK, currentOrders)
        }
    }

    post("/accept/order") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.receive<Long>()

            orderRepository.acceptOrder(orderId = orderId, shopId = shopId)

            call.respond(HttpStatusCode.Created)
        }
    }

    post("/cancel/order") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val request = call.receive<CancelBySellerRequest>()

            orderRepository.cancelBySeller(
                orderId = request.orderId,
                shopId = shopId,
                message = request.message,
            )

            call.respond(HttpStatusCode.Created)
        }
    }

    post("/finish/order") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.receive<Long>()

            orderRepository.finishOrder(
                orderId = orderId,
                shopId = shopId,
            )

            call.respond(HttpStatusCode.Created)
        }
    }
}
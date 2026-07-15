package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.domain.ObserveOrdersRepository
import com.ducks.features.coffeeshops.seller.domain.SellerOrdersRepository
import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.features.coffeeshops.seller.routings.request.orders.CancelBySellerRequest
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Route.sellerOrdersRoute() {
    val orderRepository by application.inject<SellerOrdersRepository> { parametersOf(application) }
    val observeOrdersRepository by application.inject<ObserveOrdersRepository>()

    get("/check/orders") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId

            val currentOrders = observeOrdersRepository.getCurrentOrders(shopId = shopId)

            call.respond(HttpStatusCode.OK, currentOrders)
        }
    }

    post("/order/accept") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.receive<Long>()

            orderRepository.acceptOrder(orderId = orderId, shopId = shopId)

            call.respond(HttpStatusCode.Created)
        }
    }

    post("/order/cancel") {
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

    // Заказ приготовлен и готов к выдаче (принят -> готов).
    post("/order/ready") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.receive<Long>()

            orderRepository.markOrderReady(
                orderId = orderId,
                shopId = shopId,
            )

            call.respond(HttpStatusCode.Created)
        }
    }

    // Выдать заказ клиенту / завершить (готов -> выдан).
    post("/order/give-out") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.receive<Long>()

            orderRepository.giveOutOrder(
                orderId = orderId,
                shopId = shopId,
            )

            call.respond(HttpStatusCode.Created)
        }
    }

    // Клиент не забрал заказ (готов -> не забран).
    post("/order/not-picked-up") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.receive<Long>()

            orderRepository.markOrderNotPickedUp(
                orderId = orderId,
                shopId = shopId,
            )

            call.respond(HttpStatusCode.Created)
        }
    }
}
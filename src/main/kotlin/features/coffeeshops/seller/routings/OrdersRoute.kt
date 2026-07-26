package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.domain.ObserveOrdersRepository
import com.ducks.features.coffeeshops.seller.domain.SellerOrdersHistoryRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun Route.sellerOrdersRoute() {
    val orderRepository by application.inject<SellerOrdersRepository> { parametersOf(application) }
    val observeOrdersRepository by application.inject<ObserveOrdersRepository>()
    val ordersHistoryRepository by application.inject<SellerOrdersHistoryRepository>()

    get("/check/orders") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId

            val currentOrders = observeOrdersRepository.getCurrentOrders(shopId = shopId)

            call.respond(HttpStatusCode.OK, currentOrders)
        }
    }

    // Заказы кофейни за конкретный день.
    // Дата передаётся в параметре date: либо yyyy-MM-dd, либо timestamp в миллисекундах.
    get("/orders") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId

            val rawDate = call.request.queryParameters["date"]
                ?: return@ducksTryCatch call.respond(
                    HttpStatusCode.BadRequest,
                    "Не передана дата дня (параметр date, формат yyyy-MM-dd)",
                )
            val day = rawDate.toLocalDateOrNull()
                ?: return@ducksTryCatch call.respond(
                    HttpStatusCode.BadRequest,
                    "Некорректная дата: $rawDate. Ожидается формат yyyy-MM-dd",
                )

            val orders = ordersHistoryRepository.getOrdersByDay(shopId = shopId, day = day)

            call.respond(HttpStatusCode.OK, orders)
        }
    }

    // Детали конкретного заказа кофейни.
    get("/order/{id}") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val orderId = call.parameters["id"]?.toLongOrNull()
                ?: return@ducksTryCatch call.respond(HttpStatusCode.BadRequest, "Некорректный id заказа")

            val order = ordersHistoryRepository.getOrderDetails(shopId = shopId, orderId = orderId)

            if (order != null) {
                call.respond(HttpStatusCode.OK, order)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
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

// Дата дня: yyyy-MM-dd либо timestamp в миллисекундах (день считается в UTC).
private fun String.toLocalDateOrNull(): LocalDate? {
    toLongOrNull()?.let {
        return Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }

    return runCatching { LocalDate.parse(this) }.getOrNull()
}
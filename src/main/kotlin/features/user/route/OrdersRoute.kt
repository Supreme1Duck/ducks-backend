package com.ducks.features.user.route

import com.ducks.features.coffeeshops.client.routings.request.CreateOrderRequest
import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.domain.ClientCreateOrdersRepository
import com.ducks.features.user.domain.ClientsOrdersRepository
import com.ducks.features.user.domain.ReorderPreviewRepository
import com.ducks.features.user.util.getClientPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Route.ordersRoute() {

    val createOrdersRepository by application.inject<ClientCreateOrdersRepository> { parametersOf(application) }
    val clientsOrdersRepository by application.inject<ClientsOrdersRepository> { parametersOf(application) }
    val reorderPreviewRepository by application.inject<ReorderPreviewRepository>()
    val usersRepository by application.inject<UsersRepository>()

    get("order/active") {
        ducksTryCatch {
            val userId = getClientPrincipal().userId

            val activeOrder = clientsOrdersRepository.getActiveOrder(userId)

            if (activeOrder != null) {
                call.respond(HttpStatusCode.OK, activeOrder)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    get("order/{id}") {
        ducksTryCatch {
            val orderId = call.parameters["id"]?.toLongOrNull()
                ?: return@ducksTryCatch call.respond(HttpStatusCode.BadRequest)
            val userId = getClientPrincipal().userId

            val order = clientsOrdersRepository.getOrder(orderId, userId)

            if (order != null) {
                call.respond(HttpStatusCode.OK, order)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    get("reorder-preview/{orderId}") {
        ducksTryCatch {
            val orderId = call.parameters["orderId"]?.toLongOrNull()
                ?: return@ducksTryCatch call.respond(HttpStatusCode.BadRequest)
            val userId = getClientPrincipal().userId

            val preview = reorderPreviewRepository.getReorderPreview(orderId, userId)

            if (preview != null) {
                call.respond(HttpStatusCode.OK, preview)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    post("order/create") {
        ducksTryCatch {
            val request = call.receive<CreateOrderRequest>()
            val clientPhoneNumber = getClientPrincipal().phoneNumber

            createOrdersRepository.createOrder(request, clientPhoneNumber)

            call.respond(HttpStatusCode.Created)
        }
    }

    get("orders") {
        ducksTryCatch {
            val userId = getClientPrincipal().userId

            val orders = clientsOrdersRepository.getOrders(userId)

            call.respond(HttpStatusCode.OK, orders)
        }
    }

    post("device-token") {
        ducksTryCatch {
            val token = call.receive<String>()
            val userId = getClientPrincipal().userId
            usersRepository.updateFcmToken(userId, token)
            call.respond(HttpStatusCode.OK)
        }
    }

    // Только непринятый заказ
    post("order/cancel") {
        ducksTryCatch {
            val orderId = call.receive<Long>()
            val clientId = getClientPrincipal().userId

            clientsOrdersRepository.cancelOrder(
                orderId = orderId,
                clientId = clientId,
            )
        }
    }
}
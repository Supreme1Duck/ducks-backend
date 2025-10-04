package com.ducks.features.user.route

import com.ducks.features.coffeeshops.client.routings.request.CreateOrderRequest
import com.ducks.features.user.domain.ClientCreateOrdersRepository
import com.ducks.features.user.domain.ClientsOrdersRepository
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

    get("order/active") {
        ducksTryCatch {
            val userId = getClientPrincipal().userId

            val activeOrder = clientsOrdersRepository.getActiveOrder(userId)

            call.respond(HttpStatusCode.OK, activeOrder)
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
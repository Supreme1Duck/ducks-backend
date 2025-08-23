package com.ducks.features.coffeeshops.client.routings

import com.ducks.features.coffeeshops.client.domain.ClientOrdersRepository
import com.ducks.features.coffeeshops.client.routings.request.CreateOrderRequest
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.ordersRoute() {

    val ordersRepository by application.inject<ClientOrdersRepository>()

    post("order/create") {
        ducksTryCatch {
            val request = call.receive<CreateOrderRequest>()

            ordersRepository.createOrder(request)

            call.respond(HttpStatusCode.Created)
        }
    }
}
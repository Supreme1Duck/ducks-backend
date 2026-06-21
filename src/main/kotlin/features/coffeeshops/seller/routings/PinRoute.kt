package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.data.SellerPinCodeDataSource
import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class PinRequest(val pin: String)

fun Route.sellerPinRoute() {
    val pinDataSource by application.inject<SellerPinCodeDataSource>()

    post("/pin/verify") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val request = call.receive<PinRequest>()

            pinDataSource.verifyPin(shopId, request.pin)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

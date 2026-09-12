package com.ducks.features.cashregister

import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Только внутри authenticate(JWT_COFFEE_SELLER_NAME). shopId берётся из JWT. */
fun Route.alfaCashRegisterRoute() {
    val repository by application.inject<AlfaCashRegisterRepository>()
    route("/cash-register/alfa") {
        get("/settings") {
            ducksTryCatch { call.respond(repository.getSettings(getCoffeeShopSellerPrincipal().shopId)) }
        }
        post("/settings") {
            ducksTryCatch {
                repository.setSettings(getCoffeeShopSellerPrincipal().shopId, call.receive())
                call.respond(HttpStatusCode.NoContent)
            }
        }
        get("/transfers") {
            ducksTryCatch { call.respond(repository.listTransfers(getCoffeeShopSellerPrincipal().shopId)) }
        }
        get("/transfers/{orderId}") {
            ducksTryCatch {
                call.respond(repository.getTransfer(
                    getCoffeeShopSellerPrincipal().shopId,
                    call.parameters["orderId"]?.toLongOrNull(),
                ))
            }
        }
        post("/transfers/{orderId}/start-sending-to-cash-register") {
            ducksTryCatch {
                call.respond(
                    repository.startOrderSendingToCashRegister(
                        getCoffeeShopSellerPrincipal().shopId,
                        call.parameters["orderId"]?.toLongOrNull(),
                        call.receive(),
                    )
                )
            }
        }
        post("/transfers/{orderId}/report-order-sending-result") {
            ducksTryCatch {
                call.respond(
                    repository.reportOrderSendingResult(
                        getCoffeeShopSellerPrincipal().shopId,
                        call.parameters["orderId"]?.toLongOrNull(),
                        call.receive(),
                    )
                )
            }
        }
    }
}

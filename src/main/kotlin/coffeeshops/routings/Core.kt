package com.ducks.coffeeshops.routings

import com.ducks.auth.JWT_COFFEE_SELLER_NAME
import com.ducks.coffeeshops.common.routings.clientRoute
import com.ducks.coffeeshops.seller.routings.sellerAuthRoute
import com.ducks.coffeeshops.seller.routings.sellersRoute
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.coffeeShopsRoute() {
    route("/coffee-shops") {
        clientRoute()

        authenticate(JWT_COFFEE_SELLER_NAME) {
            sellersRoute()
        }

        sellerAuthRoute()
    }
}
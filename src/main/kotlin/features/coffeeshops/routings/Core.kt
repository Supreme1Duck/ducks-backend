package com.ducks.features.coffeeshops.routings

import com.ducks.auth.JWT_COFFEE_SELLER_NAME
import com.ducks.features.coffeeshops.client.routings.clientRoute
import com.ducks.features.coffeeshops.seller.routings.sellerOrdersRoute
import com.ducks.coffeeshops.seller.routings.sellerAuthRoute
import com.ducks.features.coffeeshops.seller.routings.sellersRoute
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.coffeeShopsRoute() {
    route("/coffee-shops") {
        clientRoute()

        authenticate(JWT_COFFEE_SELLER_NAME) {
            sellersRoute()
            sellerOrdersRoute()
        }

        sellerAuthRoute()
    }
}
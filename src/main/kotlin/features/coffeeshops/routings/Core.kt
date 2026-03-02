package com.ducks.features.coffeeshops.routings

import com.ducks.auth.JWT_COFFEE_SELLER_NAME
import com.ducks.features.coffeeshops.seller.routings.sellerAuthRoute
import com.ducks.features.coffeeshops.client.routings.clientRoute
import com.ducks.features.coffeeshops.seller.routings.sellerOrdersRoute
import com.ducks.features.coffeeshops.seller.routings.sellersRoute
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Route.coffeeShopsRoute() {
    route("/coffee-shops") {
        clientRoute()

        authenticate(JWT_COFFEE_SELLER_NAME) {
            route("/seller") {
                sellersRoute()
                sellerOrdersRoute()
            }
        }

        sellerAuthRoute()

        staticFiles(
            remotePath = "/products/images",
            dir = File("coffee-shops/products/images")
        )

        staticFiles(
            remotePath = "/images",
            dir = File("coffee-shops/images")
        )
    }
}
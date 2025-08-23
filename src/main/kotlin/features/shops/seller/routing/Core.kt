package com.ducks.features.shops.seller.routing

import com.ducks.auth.JWT_SELLER_NAME
import com.ducks.features.shops.seller.routing.routes.authRoute
import com.ducks.features.shops.seller.routing.routes.productsRoute
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.sellersRoute() {

    route("/seller") {
        authRoute()

        authenticate(JWT_SELLER_NAME) {
            productsRoute()
        }
    }
}
package com.ducks.features.shops.common.routings

import com.ducks.features.shops.seller.routing.sellersRoute
import io.ktor.server.routing.*

fun Route.shopsRoute() {
    route("/shops") {
        productsRoute()
        shopsListRoute()
        sellersRoute()
        getImageRoute()
    }
}
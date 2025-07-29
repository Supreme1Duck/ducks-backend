package com.ducks.shops.common.routings

import com.ducks.shops.seller.routing.sellersRoute
import io.ktor.server.routing.*

fun Route.shopsRoute() {
    route("/shops") {
        productsRoute()
        shopsListRoute()
        sellersRoute()
        getImageRoute()
    }
}
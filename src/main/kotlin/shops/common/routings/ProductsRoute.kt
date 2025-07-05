package com.ducks.shops.common.routings

import io.ktor.server.routing.*

fun Route.productsRoute() {
    route("/products") {
        searchProductsRoute()
    }
}
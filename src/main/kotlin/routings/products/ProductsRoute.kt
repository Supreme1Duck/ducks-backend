package com.ducks.routings.products

import io.ktor.server.routing.*

fun Route.productsRoute() {
    route("/products") {
        searchProductsRoute()
    }
}
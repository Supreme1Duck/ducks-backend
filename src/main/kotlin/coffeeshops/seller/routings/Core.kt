package com.ducks.coffeeshops.seller.routings

import io.ktor.server.routing.*

fun Route.sellersRoute() {
    route("/seller") {
        shopsAndProductsRoute()
        constructorsRoute()
        productCategoriesRoute()
    }
}
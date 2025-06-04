package com.ducks.routings.products

import io.ktor.server.routing.*

fun Route.shopsRoute() {
    route("/shops") {
        productsRoute()
        getShopsListRoute()
    }
}
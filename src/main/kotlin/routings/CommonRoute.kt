package com.ducks.routings

import com.ducks.routings.products.shopsRoute
import io.ktor.server.routing.*

fun Route.commonRoute() {
    shopsRoute()
}
package com.ducks.routings

import com.ducks.shops.common.routings.shopsRoute
import io.ktor.server.routing.*

fun Route.commonRoute() {
    shopsRoute()
}
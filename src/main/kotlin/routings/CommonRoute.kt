package com.ducks.routings

import com.ducks.features.coffeeshops.routings.coffeeShopsRoute
import com.ducks.features.user.route.userRoute
import com.ducks.features.shops.common.routings.shopsRoute
import io.ktor.server.routing.*

fun Route.commonRoute() {
    userRoute()
    shopsRoute()
    coffeeShopsRoute()
}
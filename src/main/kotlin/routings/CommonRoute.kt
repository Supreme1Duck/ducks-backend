package com.ducks.routings

import com.ducks.features.applink.route.appLinkRoute
import com.ducks.features.coffeeshops.routings.coffeeShopsRoute
import com.ducks.features.config.route.clientConfigRoute
import com.ducks.features.legal.route.legalRoute
import com.ducks.features.user.route.userRoute
import com.ducks.features.shops.common.routings.shopsRoute
import io.ktor.server.routing.*

fun Route.commonRoute() {
    clientConfigRoute()
    userRoute()
    shopsRoute()
    coffeeShopsRoute()
    legalRoute()
    appLinkRoute()
}
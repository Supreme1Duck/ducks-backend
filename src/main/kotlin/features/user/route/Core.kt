package com.ducks.features.user.route

import io.ktor.server.routing.*

fun Route.userRoute() {

    route("/users") {
        authRoute()

        ordersRoute()
    }
}
package com.ducks.features.user.route

import com.ducks.auth.JWT_CLIENT_NAME
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.userRoute() {

    route("/users") {
        authRoute()

        authenticate(JWT_CLIENT_NAME) {
            ordersRoute()
        }
    }
}
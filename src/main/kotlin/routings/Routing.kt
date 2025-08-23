package com.ducks.routings

import com.ducks.admin.route.adminRoute
import com.ducks.auth.JWT_ADMIN_NAME
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        commonRoute()

        authenticate(JWT_ADMIN_NAME) {
            adminRoute()
        }
    }
}
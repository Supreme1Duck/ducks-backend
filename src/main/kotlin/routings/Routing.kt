package com.ducks.routings

import com.ducks.admin.route.adminRoute
import com.ducks.auth.JWT_ADMIN_NAME
import com.ducks.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val userRepository by inject<UserRepository>()

    routing {
        clientLoginRoute(userRepository)

        commonRoute()

        authenticate(JWT_ADMIN_NAME) {
            adminRoute()
        }
    }
}
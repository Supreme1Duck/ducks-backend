package com.ducks.routings

import com.ducks.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val userRepository by inject<UserRepository>()

    routing {
        loginRoute(userRepository)

        commonRoute()

        authenticate {
            mainRoute()
        }
    }
}
package com.ducks.routings

import com.ducks.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val userRepository by inject<UserRepository>()

    routing {
        loginRoute(userRepository)

        authenticate {
            route("/api") {
                get("/nothing") {
                    val phone = call.principal<JWTPrincipal>()?.payload?.getClaim("userPhoneNumber")?.asString()

                    println("User phone number -> $phone")

                    call.respond(HttpStatusCode.OK, "Hello Andrew Duck!")
                }
            }
        }
    }
}
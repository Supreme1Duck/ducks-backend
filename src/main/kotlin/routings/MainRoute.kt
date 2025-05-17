package com.ducks.routings

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.mainRoute() {
    route("/api") {
        get("/nothing") {
            val phone = call.principal<JWTPrincipal>()?.payload?.getClaim("userPhoneNumber")?.asString()

            call.respond(HttpStatusCode.OK, "Hello Andrew Duck!")
        }
    }
}
package com.ducks.plugin

import com.ducks.service.JwtService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Application.installAuth() {
    val jwtService by inject<JwtService> { parametersOf(this) }

    authentication {
        jwt {
            realm = jwtService.realm

            verifier(
                verifier = jwtService.verifier
            )

            validate { credential ->
                jwtService.customValidator(credential = credential)
            }

            challenge {defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Ты дебил")
            }
        }
    }
}
package com.ducks.features.user.util

import com.ducks.auth.client.JWTClientPrincipal
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun RoutingContext.getClientPrincipal() : JWTClientPrincipal {
    return call.principal<JWTClientPrincipal>()!!
}
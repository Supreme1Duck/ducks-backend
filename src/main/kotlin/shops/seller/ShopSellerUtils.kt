package com.ducks.shops.seller

import com.ducks.auth.seller.JWTSellerPrincipal
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun RoutingContext.getSellerPrincipal(): JWTSellerPrincipal {
    return call.principal<JWTSellerPrincipal>()!!
}
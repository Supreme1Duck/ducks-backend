package com.ducks.coffeeshops.seller

import com.ducks.auth.coffee_seller.JWTCoffeeSellerPrincipal
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun RoutingContext.getCoffeeShopSellerPrincipal(): JWTCoffeeSellerPrincipal {
    return call.principal<JWTCoffeeSellerPrincipal>()!!
}
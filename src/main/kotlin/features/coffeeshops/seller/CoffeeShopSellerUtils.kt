package com.ducks.features.coffeeshops.seller

import com.ducks.auth.coffee_seller.JWTCoffeeSellerPrincipal
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

fun RoutingContext.getCoffeeShopSellerPrincipal(): JWTCoffeeSellerPrincipal {
    return call.principal<JWTCoffeeSellerPrincipal>()!!
}

fun WebSocketServerSession.getCoffeeShopSellerPrincipal(): JWTCoffeeSellerPrincipal {
    return call.principal<JWTCoffeeSellerPrincipal>()!!
}
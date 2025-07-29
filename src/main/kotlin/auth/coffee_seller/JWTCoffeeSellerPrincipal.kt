package com.ducks.auth.coffee_seller

import com.auth0.jwt.interfaces.Payload
import com.ducks.auth.coffee_seller.JWTCoffeeSellerService.Companion.COFFEE_SHOP_ID_CLAIM
import io.ktor.server.auth.jwt.*

class JWTCoffeeSellerPrincipal(
    principalPayload: Payload
) : JWTPayloadHolder(principalPayload) {

    val shopId: Long = principalPayload.getClaim(COFFEE_SHOP_ID_CLAIM).asLong()
}
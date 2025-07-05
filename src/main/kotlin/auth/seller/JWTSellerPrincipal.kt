package com.ducks.auth.seller

import com.auth0.jwt.interfaces.Payload
import com.ducks.auth.seller.JWTSellerService.Companion.SHOP_ID_CLAIM
import io.ktor.server.auth.jwt.*

class JWTSellerPrincipal(
    principalPayload: Payload
) : JWTPayloadHolder(principalPayload) {

    val shopId: Long = principalPayload.getClaim(SHOP_ID_CLAIM).asLong()
}
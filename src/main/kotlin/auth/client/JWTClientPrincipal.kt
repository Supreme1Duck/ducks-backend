package com.ducks.auth.client

import com.auth0.jwt.interfaces.Payload
import com.ducks.auth.client.JWTClientService.Companion.ID_CLAIM
import io.ktor.server.auth.jwt.*

class JWTClientPrincipal(
    principalPayload: Payload
) : JWTPayloadHolder(principalPayload) {

    // TODO вернуть, когда вернётся вход по номеру телефона.
//    val phoneNumber = principalPayload.getClaim(PHONE_CLAIM).asString()
    val userId = principalPayload.getClaim(ID_CLAIM).asLong()
}

package com.ducks.auth.client

import com.auth0.jwt.interfaces.Payload
import com.ducks.auth.client.JWTClientService.Companion.PHONE_CLAIM
import io.ktor.server.auth.jwt.*

class JWTClientPrincipal(
    principalPayload: Payload
) : JWTPayloadHolder(principalPayload) {

    val phoneNumber = principalPayload.getClaim(PHONE_CLAIM)

}
package com.ducks.auth.admin

import com.auth0.jwt.interfaces.Payload
import com.ducks.auth.admin.JWTAdminService.Companion.ADMIN_ID_CLAIM
import io.ktor.server.auth.jwt.*

class JWTAdminPrincipal(
    principalPayload: Payload
) : JWTPayloadHolder(principalPayload) {

    val adminId: Long = principalPayload.getClaim(ADMIN_ID_CLAIM).asLong()

}
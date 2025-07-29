package com.ducks.auth.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.auth.DucksJWTService
import com.ducks.repository.UserRepository
import com.ducks.auth.Roles
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JWTClientService(
    application: Application,
    private val userRepository: UserRepository,
) : DucksJWTService(application) {
    companion object {
        const val PHONE_CLAIM = "userPhoneNumberClaim"
    }

    private fun extractPhoneNumber(credential: JWTCredential): String? {
        return credential.payload.getClaim(PHONE_CLAIM)?.asString()
    }

    fun customValidator(credential: JWTCredential): JWTClientPrincipal? {
        val userPhone = extractPhoneNumber(credential)
        val user = userPhone?.let(userRepository::getUserByPhoneNumber)

        return user?.let {
            if (audienceMatches(credential)) {
                JWTClientPrincipal(credential.payload)
            } else null
        }
    }

    fun generateClientToken(phoneNumber: String): String? {
        return JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(ROLE_CLAIM, Roles.Client.role)
            .withClaim(PHONE_CLAIM, phoneNumber)
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }
}
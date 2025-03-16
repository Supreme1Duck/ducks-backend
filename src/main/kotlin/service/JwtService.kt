package com.ducks.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JwtService(
    private val application: Application,
    private val userRepository: UserRepository,
) {

    companion object {
        private const val PHONE_CLAIM = "userPhoneNumber"
    }

    private val secret = getConfigProperty("jwt.secret")
    private val issuer = getConfigProperty("jwt.issuer")
    private val audience = getConfigProperty("jwt.audience")

    val realm = getConfigProperty("jwt.realm")

    val verifier: JWTVerifier = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun createJwtToken(phoneNumber: String): String? {
        return JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(PHONE_CLAIM, phoneNumber)
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }

    fun customValidator(credential: JWTCredential): JWTPrincipal? {
        val userPhone = extractPhoneNumber(credential)
        val user = userPhone?.let(userRepository::getUserByPhoneNumber)

        return user?.let {
            if (audienceMatches(credential)) {
                JWTPrincipal(credential.payload)
            } else null
        }
    }

    private fun audienceMatches(credential: JWTCredential): Boolean {
        return credential.audience.contains(audience)
    }

    private fun extractPhoneNumber(credential: JWTCredential): String? {
        return credential.payload.getClaim(PHONE_CLAIM)?.asString()
    }

    private fun getConfigProperty(path: String) = application.environment.config.property(path).getString()
}
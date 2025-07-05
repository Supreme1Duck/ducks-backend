package com.ducks.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*

abstract class DucksJWTService(
    private val application: Application,
) {
    companion object {
        const val ROLE_CLAIM = "roleClaim"
    }

    val secret = getConfigProperty("jwt.secret")
    val issuer = getConfigProperty("jwt.issuer")
    val audience = getConfigProperty("jwt.audience")

    val realm = getConfigProperty("jwt.realm")

    val verifier: JWTVerifier = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun audienceMatches(credential: JWTCredential): Boolean {
        return credential.audience.contains(audience)
    }

    private fun getConfigProperty(path: String) = application.environment.config.property(path).getString()
}
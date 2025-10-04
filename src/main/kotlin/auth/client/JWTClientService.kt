package com.ducks.auth.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.auth.DucksJWTService
import com.ducks.auth.Roles
import com.ducks.features.user.data.UsersRepository
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.runBlocking
import java.util.*

class JWTClientService(
    application: Application,
    private val userRepository: UsersRepository,
) : DucksJWTService(application) {
    companion object {
        const val PHONE_CLAIM = "userPhoneNumberClaim"
        const val ID_CLAIM = "userIdClaim"
    }

    private fun extractPhoneNumber(credential: JWTCredential): String? {
        return credential.payload.getClaim(PHONE_CLAIM)?.asString()
    }

    private fun extractUserId(credential: JWTCredential): Long? {
        return credential.payload.getClaim(ID_CLAIM)?.asLong()
    }

    fun customValidator(credential: JWTCredential): JWTClientPrincipal? {
        val userPhone = extractPhoneNumber(credential)
        val userId = extractUserId(credential)

        if (userId == null || userPhone == null)
            return null

        val user = runBlocking {
            userRepository.getUserByCredentials(userId, userPhone)
        }

        return user?.let {
            if (audienceMatches(credential)) {
                JWTClientPrincipal(credential.payload)
            } else null
        }
    }

    fun generateClientToken(userId: Long, phoneNumber: String): String? {
        return JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(ROLE_CLAIM, Roles.Client.role)
            .withClaim(PHONE_CLAIM, phoneNumber)
            .withClaim(ID_CLAIM, userId)
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }
}
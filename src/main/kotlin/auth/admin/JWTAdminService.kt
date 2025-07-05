package com.ducks.auth.admin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.admin.repository.AdminRepository
import com.ducks.auth.DucksJWTService
import com.ducks.auth.Roles
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JWTAdminService(
    application: Application,
    private val adminRepository: AdminRepository,
) : DucksJWTService(application) {
    companion object {
        const val ADMIN_ID_CLAIM = "adminIdClaim"
    }

    private fun extractAdminId(credential: JWTCredential): Long? {
        return credential.payload.getClaim(ADMIN_ID_CLAIM)?.asLong()
    }

    fun customValidator(credential: JWTCredential): JWTAdminPrincipal? {
        val adminId = extractAdminId(credential)
        val isAdminExists = adminId?.let(adminRepository::isAdminExists) ?: false

        return if (
            isAdminExists &&
            audienceMatches(credential) &&
            credential.getClaim(ROLE_CLAIM, String::class) == Roles.Admin.role
        ) {
            JWTAdminPrincipal(credential.payload)
        } else {
            null
        }
    }

    fun generateAdminToken(
        adminId: Long,
    ): String? {
        return JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(ROLE_CLAIM, Roles.Admin.role)
            .withClaim(ADMIN_ID_CLAIM, adminId)
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }
}
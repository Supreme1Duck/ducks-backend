package com.ducks.auth.seller

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.auth.DucksJWTService
import com.ducks.auth.Roles
import com.ducks.shops.common.repository.ShopsRepository
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JWTSellerService(
    application: Application,
    private val shopsRepository: ShopsRepository,
) : DucksJWTService(application) {

    companion object {
        const val SHOP_ID_CLAIM = "shopIdClaim"
    }

    private fun extractShopId(credential: JWTCredential): Long? {
        return credential.payload.getClaim(SHOP_ID_CLAIM).asLong()
    }

    suspend fun customValidator(credential: JWTCredential): JWTSellerPrincipal? {
        val shopId = extractShopId(credential) ?: return null
        val isShopExists = shopsRepository.exists(shopId)

        return if (audienceMatches(credential) && isShopExists) {
            JWTSellerPrincipal(credential.payload)
        } else null
    }

    fun generateSellerToken(shopId: Long): String? {
        return JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(ROLE_CLAIM, Roles.Seller.role)
            .withClaim(SHOP_ID_CLAIM, shopId)
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }
}
package com.ducks.auth.coffee_seller

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.auth.DucksJWTService
import com.ducks.auth.Roles
import com.ducks.coffeeshops.common.domain.CoffeeShopsRepository
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JWTCoffeeSellerService(
    application: Application,
    private val shopsRepository: CoffeeShopsRepository,
) : DucksJWTService(application) {

    companion object {
        const val COFFEE_SHOP_ID_CLAIM = "coffeeShopIdClaim"
    }

    private fun extractShopId(credential: JWTCredential): Long? {
        return credential.payload.getClaim(COFFEE_SHOP_ID_CLAIM).asLong()
    }

    suspend fun customValidator(credential: JWTCredential): JWTCoffeeSellerPrincipal? {
        val shopId = extractShopId(credential) ?: return null
        val isShopExists = shopsRepository.exists(shopId)

        return if (audienceMatches(credential) && isShopExists) {
            JWTCoffeeSellerPrincipal(credential.payload)
        } else null
    }

    fun generateSellerToken(shopId: Long): String? {
        return JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(ROLE_CLAIM, Roles.CoffeeSeller.role)
            .withClaim(COFFEE_SHOP_ID_CLAIM, shopId)
            .withIssuedAt(Date())
            .sign(Algorithm.HMAC256(secret))
    }
}
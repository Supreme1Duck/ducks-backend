package com.ducks.features.cashregister

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ducks.auth.coffee_seller.JWTCoffeeSellerPrincipal
import com.ducks.auth.coffee_seller.JWTCoffeeSellerService.Companion.COFFEE_SHOP_ID_CLAIM
import com.ducks.util.ducksJson
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.*

class AlfaCashRegisterRouteTest {
    private val algorithm = Algorithm.HMAC256("alfa-cash-register-route-test-only")
    private val token = JWT.create().withClaim(COFFEE_SHOP_ID_CLAIM, 1L).sign(algorithm)
    private val prefix = "/cash-register/alfa"

    private fun ApplicationTestBuilder.configureRoutes() {
        application {
            install(ContentNegotiation) { json(ducksJson) }
            install(Koin) { modules(module { single { AlfaCashRegisterRepository() } }) }
            install(Authentication) {
                jwt("test-seller") {
                    verifier(JWT.require(algorithm).build())
                    validate { JWTCoffeeSellerPrincipal(it.payload) }
                }
            }
            routing { authenticate("test-seller") { alfaCashRegisterRoute() } }
        }
    }

    @Test
    fun `без авторизации недоступны настройки очередь и изменение передачи`() = testApplication {
        configureRoutes()
        for (path in listOf("/settings", "/transfers", "/transfers/11")) {
            assertEquals(HttpStatusCode.Unauthorized, client.get(prefix + path).status)
        }
        assertEquals(HttpStatusCode.Unauthorized, client.post("$prefix/settings").status)
        for (path in listOf("start-sending-to-cash-register", "report-order-sending-result")) {
            assertEquals(HttpStatusCode.Unauthorized, client.post("$prefix/transfers/11/$path").status)
        }
    }

    @Test
    fun `неверный id проверяется репозиторием`() = testApplication {
        configureRoutes()
        assertEquals(HttpStatusCode.BadRequest, client.get("$prefix/transfers/not-a-number") { bearerAuth(token) }.status)
    }
}

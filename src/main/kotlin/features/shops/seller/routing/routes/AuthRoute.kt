package com.ducks.features.shops.seller.routing.routes

import com.ducks.admin.api.ShopCredentialsRepository
import com.ducks.auth.SellerLoginRequest
import com.ducks.auth.seller.JWTSellerService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Route.authRoute() {
    val jwtService by application.inject<JWTSellerService> { parametersOf(application) }
    val shopCredentialsRepository by application.inject<ShopCredentialsRepository>()

    post("/auth") {
        try {
            val request = call.receive<SellerLoginRequest>()

            val shopId = shopCredentialsRepository.login(request.login, request.password)

            val token = jwtService.generateSellerToken(shopId)

            call.respond(token.toString())
        } catch (e: Exception) {
            call.respond(e.printStackTrace())
        }
    }
}
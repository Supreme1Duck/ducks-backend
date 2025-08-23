package com.ducks.coffeeshops.seller.routings

import com.ducks.admin.api.CoffeeShopCredentialsRepository
import com.ducks.admin.repository.result.LoginResult
import com.ducks.auth.SellerLoginRequest
import com.ducks.auth.coffee_seller.JWTCoffeeSellerService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Route.sellerAuthRoute() {
    val jwtService by application.inject<JWTCoffeeSellerService> { parametersOf(application) }
    val shopCredentialsRepository by application.inject<CoffeeShopCredentialsRepository>()

    post("/auth") {
        try {
            val request = call.receive<SellerLoginRequest>()

            val result = shopCredentialsRepository.login(request.login, request.password)

            when (result) {
                LoginResult.InvalidCredentials -> {
                    call.respond(HttpStatusCode.BadRequest)
                }

                is LoginResult.Success -> {
                    val token = jwtService.generateSellerToken(result.shopId)

                    call.respond(token.toString())
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.printStackTrace())
        }
    }
}
package com.ducks.plugin

import com.ducks.auth.JWT_ADMIN_NAME
import com.ducks.auth.JWT_CLIENT_NAME
import com.ducks.auth.JWT_COFFEE_SELLER_NAME
import com.ducks.auth.JWT_SELLER_NAME
import com.ducks.auth.admin.JWTAdminService
import com.ducks.auth.client.JWTClientService
import com.ducks.auth.coffee_seller.JWTCoffeeSellerService
import com.ducks.auth.seller.JWTSellerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Application.installAuth() {
    val adminJwtService by inject<JWTAdminService> { parametersOf(this) }
    val sellerJwtService by inject<JWTSellerService> { parametersOf(this) }
    val coffeeSellerService by inject<JWTCoffeeSellerService> { parametersOf(this) }
    val clientJwtService by inject<JWTClientService> { parametersOf(this) }

    authentication {
        jwt(name = JWT_CLIENT_NAME) {
            realm = clientJwtService.realm

            verifier(
                verifier = clientJwtService.verifier
            )

            validate { credential ->
                clientJwtService.customValidator(credential = credential)
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

        jwt(name = JWT_SELLER_NAME) {
            realm = sellerJwtService.realm

            verifier(
                verifier = sellerJwtService.verifier
            )

            validate { credential ->
                sellerJwtService.customValidator(credential = credential)
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

        jwt(name = JWT_ADMIN_NAME) {
            realm = adminJwtService.realm

            verifier(
                verifier = adminJwtService.verifier
            )

            validate { credential ->
                adminJwtService.customValidator(credential = credential)
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }

        jwt(name = JWT_COFFEE_SELLER_NAME) {
            realm = coffeeSellerService.realm

            verifier(
                verifier = coffeeSellerService.verifier
            )

            validate { credential ->
                coffeeSellerService.customValidator(credential = credential)
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
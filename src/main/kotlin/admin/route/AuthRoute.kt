package com.ducks.admin.route

import com.ducks.admin.repository.AdminRepository
import com.ducks.admin.repository.result.LoginResult
import com.ducks.auth.SellerLoginRequest
import com.ducks.auth.admin.JWTAdminService
import com.ducks.features.shops.seller.routing.response.SignInResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Route.adminAuthRoute() {
    val jwtService by application.inject<JWTAdminService> { parametersOf(application) }
    val adminRepository by application.inject<AdminRepository>()

    post("/admin/login") {
        try {
            val request = call.receive<SellerLoginRequest>()

            val result = adminRepository.login(request.login, request.password)

            when (result) {
                LoginResult.InvalidCredentials -> call.respond(HttpStatusCode.BadRequest)
                is LoginResult.Success -> {
                    val token = jwtService.generateAdminToken(result.shopId)
                    call.respond(SignInResponse(id = result.shopId, token = token.toString()))
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

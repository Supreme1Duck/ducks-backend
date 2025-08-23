package com.ducks.features.user.route

import com.ducks.auth.client.JWTClientService
import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.route.request.LoginRequest
import com.ducks.features.user.route.request.OtpRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

fun Route.authRoute() {
    val jwtService by application.inject<JWTClientService> { parametersOf(application) }
    val userRepository by application.inject<UsersRepository>()

    // TODO вставить реальный запрос отп
    post("/otp/generate") {
        try {
            val request = call.receive<OtpRequest>()

            userRepository.generateOtp(request)

            call.respond(message = HttpStatusCode.Created)
        } catch (e: Exception) {
            call.respond(status = HttpStatusCode.InternalServerError, message = "${e.message}")
        }
    }

    post("/otp/verify") {
        val request = call.receive<LoginRequest>()

        if (request.otp == "1234") {
            userRepository.saveUser(request)
            val token = jwtService.generateClientToken(request.phoneNumber)

            call.respond(message = token.toString(), status = HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}
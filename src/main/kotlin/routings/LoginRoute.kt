package com.ducks.routings

import com.ducks.mapper.UserMapper
import com.ducks.repository.UserRepository
import com.ducks.routings.request.LoginRequest
import com.ducks.routings.request.OtpRequest
import com.ducks.service.JwtService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.loginRoute(
    userRepository: UserRepository
) {
    val jwtService by application.inject<JwtService>()

    post("/otp/generate") {
        val request = call.receive<OtpRequest>()

        try {
            call.respond(message = HttpStatusCode.Created)
        } catch (e: Exception) {
            call.respond(status = HttpStatusCode.InternalServerError, message = "${e.message}")
        }
    }

    post("/otp/verify") {
        val request = call.receive<LoginRequest>()

        if (request.otp == "1234") {
            val user = UserMapper.loginRequestToUser(request)

            userRepository.saveUser(user)
            val token = jwtService.createJwtToken(user.phoneNumber)

            call.respond(message = token.toString(), status = HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}
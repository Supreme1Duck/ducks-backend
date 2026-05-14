package com.ducks.features.user.route

import com.ducks.auth.client.JWTClientService
import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.ratelimit.OtpRateLimiter
import com.ducks.features.user.route.request.LoginRequest
import com.ducks.features.user.route.request.OtpRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject

private fun rateLimitMessage(waitSeconds: Long): String {
    val minutes = (waitSeconds + 59) / 60
    return "Повторная операция возможна через $minutes ${minuteWord(minutes)}."
}

private fun minuteWord(n: Long): String = when {
    n % 100 in 11..19 -> "минут"
    n % 10 == 1L -> "минуту"
    n % 10 in 2..4 -> "минуты"
    else -> "минут"
}

fun Route.authRoute() {
    val jwtService by application.inject<JWTClientService> { parametersOf(application) }
    val userRepository by application.inject<UsersRepository>()
    val otpRateLimiter by application.inject<OtpRateLimiter>()

    // TODO вставить реальный запрос отп
    post("/otp/generate") {
        try {
            val request = call.receive<OtpRequest>()
            val ip = call.request.headers["X-Forwarded-For"]?.split(",")?.first()?.trim()
                ?: call.request.local.remoteAddress

            val ipWait = otpRateLimiter.checkIpLimit(ip)
            if (ipWait != null) {
                call.response.headers.append(HttpHeaders.RetryAfter, ipWait.toString())
                call.respond(HttpStatusCode.TooManyRequests, rateLimitMessage(ipWait))
                return@post
            }

            val phoneWait = otpRateLimiter.checkPhoneLimit(request.phoneNumber)
            if (phoneWait != null) {
                call.response.headers.append(HttpHeaders.RetryAfter, phoneWait.toString())
                call.respond(HttpStatusCode.TooManyRequests, rateLimitMessage(phoneWait))
                return@post
            }

            otpRateLimiter.recordIpRequest(ip)
            otpRateLimiter.recordPhoneRequest(request.phoneNumber)

            userRepository.generateOtp(request)

            call.respond(message = HttpStatusCode.Created)
        } catch (e: Exception) {
            call.respond(status = HttpStatusCode.InternalServerError, message = "${e.message}")
        }
    }

    post("/otp/verify") {
        val request = call.receive<LoginRequest>()

        if (request.otp == "123456") {
            val userId = userRepository.saveUserAndGetId(request = request)
            val token = jwtService.generateClientToken(
                userId = userId,
                phoneNumber = request.phoneNumber,
            )

            call.respond(message = token.toString(), status = HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}
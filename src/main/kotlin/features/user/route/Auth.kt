package com.ducks.features.user.route

import com.ducks.auth.client.JWTClientService
import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.ratelimit.LoginRateLimiter
import com.ducks.features.user.route.request.DeviceLoginRequest
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

// TODO вернуть вход по номеру телефона (код ниже закомментирован), пока клиент входит анонимно.
fun Route.authRoute() {
    val jwtService by application.inject<JWTClientService> { parametersOf(application) }
    val userRepository by application.inject<UsersRepository>()
    val loginRateLimiter by application.inject<LoginRateLimiter>()
//    val otpRateLimiter by application.inject<OtpRateLimiter>()
//    val otpService by application.inject<OtpService>()

    // TODO вернуть логику входа (закомментирована), пока вход идёт через /login/device.
    post("/login") {
//        val request = call.receive<DeviceLoginRequest>()
//        val deviceId = request.deviceId.trim()
//
//        if (deviceId.isEmpty() || deviceId.length > MAX_DEVICE_ID_LENGTH) {
//            return@post call.respond(HttpStatusCode.BadRequest, "Некорректный id устройства")
//        }
//
//        val userId = userRepository.saveDeviceUserAndGetId(request.copy(deviceId = deviceId))
//        val token = jwtService.generateClientToken(userId = userId)
//
//        call.respond(message = token.toString(), status = HttpStatusCode.OK)
    }

    // Временный анонимный вход: существует, пока отключена авторизация по номеру телефона.
    // Каждый вызов создаёт новый аккаунт — восстановить старый без токена нельзя.
    post("/login/device") {
        val request = call.receive<DeviceLoginRequest>()

        // Адрес берём только из соединения: прокси перед сервером нет, и X-Forwarded-For
        // присылает сам клиент — подменяя его на каждый запрос, лимит обходился бы даром.
        // Появится прокси — адрес придётся брать из заголовка, который выставляет он.
        val ip = call.request.local.remoteAddress

        // С локальной машины лимит не нужен: так удобнее гонять вход при разработке.
        val isLocalhost = ip in listOf("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")

        if (!isLocalhost) {
            val wait = loginRateLimiter.checkIpLimit(ip)
            if (wait != null) {
                call.response.headers.append(HttpHeaders.RetryAfter, wait.toString())
                return@post call.respond(HttpStatusCode.TooManyRequests, rateLimitMessage(wait))
            }
        }

        loginRateLimiter.recordIpRequest(ip)

        val userId = userRepository.createAnonymousUserAndGetId(request)
        val token = jwtService.generateClientToken(userId = userId)

        call.respond(message = token.toString(), status = HttpStatusCode.OK)
    }

//    post("/otp/generate") {
//        try {
//            val request = call.receive<OtpRequest>()
//            val ip = call.request.headers["X-Forwarded-For"]?.split(",")?.first()?.trim()
//                ?: call.request.local.remoteAddress
//
//            val isLocalhost = call.request.local.localHost in listOf("localhost", "127.0.0.1", "::1")
//
//            if (!isLocalhost) {
//                val ipWait = otpRateLimiter.checkIpLimit(ip)
//                if (ipWait != null) {
//                    call.response.headers.append(HttpHeaders.RetryAfter, ipWait.toString())
//                    call.respond(HttpStatusCode.TooManyRequests, rateLimitMessage(ipWait))
//                    return@post
//                }
//
//                val phoneWait = otpRateLimiter.checkPhoneLimit(request.phoneNumber)
//                if (phoneWait != null) {
//                    call.response.headers.append(HttpHeaders.RetryAfter, phoneWait.toString())
//                    call.respond(HttpStatusCode.TooManyRequests, rateLimitMessage(phoneWait))
//                    return@post
//                }
//            }
//
//            otpRateLimiter.recordIpRequest(ip)
//
//            if (!otpService.sendCode(request.phoneNumber)) {
//                return@post call.respond(
//                    HttpStatusCode.BadGateway,
//                    "Не удалось отправить код. Попробуйте ещё раз.",
//                )
//            }
//
//            otpRateLimiter.recordPhoneRequest(request.phoneNumber)
//
//            call.respond(message = HttpStatusCode.Created)
//        } catch (e: Exception) {
//            call.respond(status = HttpStatusCode.InternalServerError, message = "${e.message}")
//        }
//    }
//
//    post("/otp/verify") {
//        val request = call.receive<LoginRequest>()
//
//        if (otpService.verify(phoneNumber = request.phoneNumber, otp = request.otp)) {
//            val userId = userRepository.saveUserAndGetId(request = request)
//            val token = jwtService.generateClientToken(
//                userId = userId,
//                phoneNumber = request.phoneNumber,
//            )
//
//            call.respond(message = token.toString(), status = HttpStatusCode.OK)
//        } else {
//            call.respond(HttpStatusCode.BadRequest)
//        }
//    }
}

package com.ducks.features.user.route

import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.domain.AccountDeletionConfirmations
import com.ducks.features.user.domain.DELETION_CONFIRMATION_HEADER
import com.ducks.features.user.domain.DeleteAccountRepository
import com.ducks.features.user.route.request.VerifyDeletionOtpRequest
import com.ducks.features.user.util.getClientPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.accountRoute() {

    val deleteAccountRepository by application.inject<DeleteAccountRepository>()
    val deletionConfirmations by application.inject<AccountDeletionConfirmations>()
    val usersRepository by application.inject<UsersRepository>()

    // Код запрашивается обычным /users/otp/generate на номер из токена. Здесь он проверяется
    // и обменивается на одноразовое подтверждение для самого удаления.
    post("account/deletion/verify-otp") {
        ducksTryCatch {
            val principal = getClientPrincipal()
            val request = call.receive<VerifyDeletionOtpRequest>()

            val blockedForSeconds = deletionConfirmations.secondsUntilAttemptsReset(principal.userId)
            if (blockedForSeconds != null) {
                call.response.headers.append(HttpHeaders.RetryAfter, blockedForSeconds.toString())
                return@ducksTryCatch call.respond(
                    HttpStatusCode.TooManyRequests,
                    "Слишком много попыток. Попробуйте позже.",
                )
            }

            val isOtpValid = usersRepository.verifyOtp(
                phoneNumber = principal.phoneNumber,
                otp = request.otp,
            )

            if (!isOtpValid) {
                deletionConfirmations.recordFailedAttempt(principal.userId)
                return@ducksTryCatch call.respond(HttpStatusCode.BadRequest, "Неверный код")
            }

            call.respond(HttpStatusCode.OK, deletionConfirmations.issue(principal.userId))
        }
    }

    // Удаление аккаунта необратимо: доступ закрывается сразу, восстановления нет,
    // вход по тому же номеру создаст новый аккаунт с нуля.
    delete("account") {
        ducksTryCatch {
            val userId = getClientPrincipal().userId

            val deletion = deleteAccountRepository.requestDeletion(
                userId = userId,
                confirmationToken = call.request.headers[DELETION_CONFIRMATION_HEADER],
            )

            call.respond(HttpStatusCode.OK, deletion)
        }
    }
}

package com.ducks.features.user.route

import com.ducks.features.user.domain.DELETION_CONFIRMATION_HEADER
import com.ducks.features.user.domain.DeleteAccountRepository
import com.ducks.features.user.util.getClientPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.accountRoute() {

    val deleteAccountRepository by application.inject<DeleteAccountRepository>()
    // TODO вернуть вместе со входом по номеру телефона. Пока его нет, подтверждение удаления
    // получить нельзя, и DELETE /account отказывает всем — анонимный аккаунт не удаляется.
//    val deletionConfirmations by application.inject<AccountDeletionConfirmations>()
//    val otpService by application.inject<OtpService>()

//    // Код запрашивается обычным /users/otp/generate на номер из токена. Здесь он проверяется
//    // и обменивается на одноразовое подтверждение для самого удаления.
//    post("account/deletion/verify-otp") {
//        ducksTryCatch {
//            val principal = getClientPrincipal()
//            val request = call.receive<VerifyDeletionOtpRequest>()
//
//            val blockedForSeconds = deletionConfirmations.secondsUntilAttemptsReset(principal.userId)
//            if (blockedForSeconds != null) {
//                call.response.headers.append(HttpHeaders.RetryAfter, blockedForSeconds.toString())
//                return@ducksTryCatch call.respond(
//                    HttpStatusCode.TooManyRequests,
//                    "Слишком много попыток. Попробуйте позже.",
//                )
//            }
//
//            val isOtpValid = otpService.verify(
//                phoneNumber = principal.phoneNumber,
//                otp = request.otp,
//            )
//
//            if (!isOtpValid) {
//                deletionConfirmations.recordFailedAttempt(principal.userId)
//                return@ducksTryCatch call.respond(HttpStatusCode.BadRequest, "Неверный код")
//            }
//
//            call.respond(HttpStatusCode.OK, deletionConfirmations.issue(principal.userId))
//        }
//    }

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

    // Удаление анонимного аккаунта — того, что завёл /users/login/device. Отдельная ручка,
    // потому что подтверждать удаление нечем: номера у аккаунта нет, СМС отправить некуда,
    // и владение аккаунтом доказывает только сам токен. Аккаунт с номером сюда не пройдёт —
    // репозиторий такие отсекает, чтобы удаление по коду из СМС нельзя было обойти.
    //
    // Пока вход по номеру отключён, это единственный рабочий способ удалить аккаунт:
    // DELETE /users/account требует подтверждение, которое сейчас взять негде.
    // Вернётся вход по номеру — ручка останется для аккаунтов без него.
    //
    // Данные стираются сразу, без отсрочки: у анонимного аккаунта нет номера, ради
    // которого она заводилась. В ответе dataRemovalAt равен requestedAt.
    delete("account/anonymous") {
        ducksTryCatch {
            val deletion = deleteAccountRepository.requestAnonymousDeletion(
                userId = getClientPrincipal().userId,
            )

            call.respond(HttpStatusCode.OK, deletion)
        }
    }
}

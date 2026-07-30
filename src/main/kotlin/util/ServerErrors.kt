package com.ducks.util

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class DucksBadRequestError(
    override val message: String?,
): Exception(message)

suspend fun RoutingContext.ducksTryCatch(
    tryLambda: suspend () -> Unit,
) {
    try {
        tryLambda()
    } catch (e: DucksBadRequestError) {
        println("$e")
        call.respond(HttpStatusCode.BadRequest, e.message.orEmpty())
    } catch (e: Exception) {
        // Текст исключения остаётся в логах: наружу он утаскивал детали бд —
        // имена таблиц, констрейнтов и значения ключей из PSQLException.
        println("$e")
        e.printStackTrace()
        call.respond(HttpStatusCode.InternalServerError, "Что-то пошло не так, попробуйте позже")
    }
}
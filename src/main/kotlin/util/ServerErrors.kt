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
        call.respond(HttpStatusCode.BadRequest, e.message.orEmpty())
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
    }
}
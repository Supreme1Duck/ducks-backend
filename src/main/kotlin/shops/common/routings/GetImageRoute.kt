package com.ducks.shops.common.routings

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.getImageRoute() {
    get("/images/{imageUrl}") {
        try {
            val filename = call.parameters["imageUrl"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "Необходимо передать урл")
                return@get
            }

            val file = File("shops/images/$filename")

            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, "Изображение не найдено")
                return@get
            }

            call.respondFile(file)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
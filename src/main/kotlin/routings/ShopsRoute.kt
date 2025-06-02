package com.ducks.routings

import com.ducks.repository.ShopsRepository
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shopsRoute() {
    route("/shops") {
        val repository by application.inject<ShopsRepository>()

        get("/list") {
            val shops = repository.getAll(10)
            call.respond(shops)
        }
    }
}
package com.ducks.admin.route

import com.ducks.admin.repository.AdminShopsRepository
import com.ducks.admin.request.CreateShopRequest
import com.ducks.auth.admin.JWTAdminPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminShopsRoute() {
    val shopsRepository by application.inject<AdminShopsRepository>()

    post("/shops/create") {
        ducksTryCatch {
            val request = call.receive<CreateShopRequest>()
            val adminId = call.principal<JWTAdminPrincipal>()!!.adminId

            shopsRepository.createNewShop(request, adminId)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
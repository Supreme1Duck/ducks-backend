package com.ducks.admin.route

import com.ducks.admin.analytics.AdminAnalytics
import com.ducks.admin.repository.AdminShopsRepository
import com.ducks.admin.repository.CreateShopResult
import com.ducks.admin.request.CreateShopRequest
import com.ducks.auth.admin.JWTAdminPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminRoute() {
    val shopsRepository by application.inject<AdminShopsRepository>()
    val adminAnalytics by application.inject<AdminAnalytics>()

    post("/shops/create") {
        try {
            val request = call.receive<CreateShopRequest>()
            val adminId = call.principal<JWTAdminPrincipal>()!!.adminId

            val result = shopsRepository.createNewShop(request, adminId)

            when (result) {
                CreateShopResult.AlreadyExists -> {
                    call.respond(HttpStatusCode.Conflict, "Магазин с таким УНП уже сущетствует")
                }

                CreateShopResult.Success -> {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        } catch (e: Exception) {
            adminAnalytics.logException(e)
            call.respond(HttpStatusCode.InternalServerError, "Ошибка создания магазина")
        }
    }
}
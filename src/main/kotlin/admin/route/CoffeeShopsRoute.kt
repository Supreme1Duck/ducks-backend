package com.ducks.admin.route

import com.ducks.admin.analytics.AdminAnalytics
import com.ducks.admin.repository.AdminCoffeeShopsRepository
import com.ducks.admin.repository.result.CreateShopResult
import com.ducks.admin.request.CreateCoffeeCategoryRequest
import com.ducks.admin.request.CreateCoffeeShopRequest
import com.ducks.auth.admin.JWTAdminPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminCoffeeShopsRoute() {
    val coffeeShopsRepository by application.inject<AdminCoffeeShopsRepository>()
    val adminAnalytics by application.inject<AdminAnalytics>()

    route("/coffee-shops") {
        post("/create") {
            try {
                val request = call.receive<CreateCoffeeShopRequest>()
                val adminId = call.principal<JWTAdminPrincipal>()!!.adminId

                val result = coffeeShopsRepository.createNewShop(request, adminId)

                when (result) {
                    CreateShopResult.AlreadyExists -> {
                        call.respond(HttpStatusCode.Conflict, "Кофешоп с таким УНП уже сущетствует")
                    }

                    CreateShopResult.Success -> {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            } catch (e: Exception) {
                adminAnalytics.logException(e)
                println("${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Ошибка создания кофешопа")
            }
        }

        post("/category/create") {
            try {
                val data = call.receive<CreateCoffeeCategoryRequest>()

                coffeeShopsRepository.insertNewCategories(data.data)

                call.respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                println("${e.message}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
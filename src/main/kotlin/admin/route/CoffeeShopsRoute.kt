package com.ducks.admin.route

import com.ducks.admin.repository.AdminCoffeeShopsRepository
import com.ducks.admin.request.CreateCoffeeCategoryRequest
import com.ducks.admin.request.CreateCoffeeShopRequest
import com.ducks.auth.admin.JWTAdminPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminCoffeeShopsRoute() {
    val coffeeShopsRepository by application.inject<AdminCoffeeShopsRepository>()

    route("/coffee-shops") {
        post("/create") {
            ducksTryCatch {
                val request = call.receive<CreateCoffeeShopRequest>()
                val adminId = call.principal<JWTAdminPrincipal>()!!.adminId

                coffeeShopsRepository.createNewShop(request, adminId)

                call.respond(HttpStatusCode.NoContent)
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
package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.data.SellerCoffeeCategoriesRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.productCategoriesRoute() {

    val categoriesDataSource by application.inject<SellerCoffeeCategoriesRepository>()

    get("/categories") {
        try {
            val categoriesList = categoriesDataSource.getCategories()

            call.respond(categoriesList)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
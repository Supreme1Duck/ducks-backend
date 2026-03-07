package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.data.SellerCoffeeCategoriesRepository
import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.util.ducksTryCatch
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.productCategoriesRoute() {

    val categoriesDataSource by application.inject<SellerCoffeeCategoriesRepository>()

    get("/categories") {
        ducksTryCatch {
            val categoriesList = categoriesDataSource.getCategories()

            call.respond(categoriesList)
        }
    }

    get("/categories/with-count") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId

            call.respond(categoriesDataSource.getCategoriesWithProductCount(shopId))
        }
    }
}
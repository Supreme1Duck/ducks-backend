package com.ducks.features.coffeeshops.seller.routings

import com.ducks.features.coffeeshops.seller.analytics.CoffeeSellerAnalyticsRepository
import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.sellerAnalyticsRoute() {

    val repository by application.inject<CoffeeSellerAnalyticsRepository>()

    get("/analytics") {
        val shopId = getCoffeeShopSellerPrincipal().shopId

        val analytics = repository.getAnalytics(shopId)

        call.respond(HttpStatusCode.OK, analytics)
    }
}
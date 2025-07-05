package com.ducks.shops.common.routings

import com.ducks.shops.common.repository.ShopsRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shopsListRoute() {
    val repository by application.inject<ShopsRepository>()

    get("/list") {
        try {
            val lastId = call.request.queryParameters["lastId"]?.toLong()
            val limit = call.request.queryParameters["limit"]?.toInt()
            val productsPreviewLimit = call.request.queryParameters["productsPreviewLimit"]?.toInt()

            val shops = repository.getAll(lastId, limit, productsPreviewLimit)
            call.respond(shops)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e.printStackTrace())
        }
    }

    get("/search") {
        try {
            val lastId = call.request.queryParameters["lastId"]?.toLong()
            val limit = call.request.queryParameters["limit"]?.toInt()
            val query = call.request.queryParameters["query"]
            val productsPreviewLimit = call.request.queryParameters["productsPreviewLimit"]?.toInt()

            val shops = repository.searchShops(
                query = query.orEmpty(),
                lastId = lastId,
                limit = limit,
                productsPreviewLimit = productsPreviewLimit
            )

            call.respond(shops)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e.printStackTrace())
        }
    }

    get("/{shopId}") {
        try {
            val shopId = call.parameters["shopId"]!!.toLong()
            val productsPreviewLimit = call.request.queryParameters["productsPreviewLimit"]?.toInt()

            val shop = repository.getById(shopId, productsPreviewLimit)

            call.respond(shop)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e.printStackTrace())
        }
    }
}
package com.ducks.shops.common.routings

import com.ducks.shops.common.repository.ShopProductsRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.searchProductsRoute() {
    val repository by application.inject<ShopProductsRepository>()

    get("/search") {
        try {
            val query = call.request.queryParameters["query"]
            val categoryIds = call.request.queryParameters["category"]?.split(",")?.map { it.trim().toLong() }
                .let { if (it.isNullOrEmpty()) null else it }

            val sizeIds = call.request.queryParameters["size"]?.split(",")?.map { it.trim().toLong() }
                .let { if (it.isNullOrEmpty()) null else it }

            val colorIds = call.request.queryParameters["colorId"]?.split(",")?.map { it.trim().toInt() }
            val seasonIds = call.request.queryParameters["seasonId"]?.split(",")?.map { it.trim().toInt() }

            val lastId = call.request.queryParameters["lastId"]?.toLong()
            val limit = call.request.queryParameters["limit"]?.toInt()

            val result = repository.searchWithQueryOrFilters(
                query = query,
                shopId = null,
                categoryIds = categoryIds,
                colorIds = colorIds,
                seasonIds = seasonIds,
                sizeIds = sizeIds,
                lastId = lastId,
                limit = limit
            )

            call.respond(result)
        } catch (e: Exception) {
            println("${e.printStackTrace()}")
            call.respond(status = HttpStatusCode.BadRequest, message = e.printStackTrace())
        }
    }
}
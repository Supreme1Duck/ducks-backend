package com.ducks.routings.products

import com.ducks.domain.SearchProductsUseCase
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.searchProductsRoute() {
    val repository by application.inject<SearchProductsUseCase>()

    get("/search") {
        try {
            val query = call.request.queryParameters["query"]
            val categoryIds = call.request.queryParameters["category"]?.split(",")?.map { it.trim().toLong() }
                .let { if (it.isNullOrEmpty()) null else it }

            val sizeIds = call.request.queryParameters["size"]?.split(",")?.map { it.trim().toLong() }
                .let { if (it.isNullOrEmpty()) null else it }

            val colorId = call.request.queryParameters["colorId"]?.toInt()
            val seasonId = call.request.queryParameters["seasonId"]?.toInt()

            println("$query, $categoryIds, $sizeIds, $colorId, $seasonId")

            val result = repository.invoke(query, categoryIds, sizeIds, colorId, seasonId)

            call.respond(result)
        } catch (e: Exception) {
            println("${e.printStackTrace()}")
            call.respond(status = HttpStatusCode.BadRequest, message = e.printStackTrace())
        }
    }
}
package com.ducks.admin.route

import com.ducks.admin.repository.AdminCoffeeShopsRepository
import com.ducks.common.data.SaveImageResult
import com.ducks.features.coffeeshops.seller.domain.CoffeeShopImageRepository
import com.ducks.util.DucksBadRequestError
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Картинка товара со стороны админки. От продавцовской заливки отличается двумя вещами:
 * товар не привязан к кофейне из токена, и ссылка сразу проставляется товару, а не
 * возвращается клиенту «на подумать» — иначе при обрыве между двумя запросами файл
 * оставался бы в хранилище ничей.
 */
fun Route.adminCoffeeProductImageRoute() {
    val coffeeShopsRepository by application.inject<AdminCoffeeShopsRepository>()
    val imagesRepository by application.inject<CoffeeShopImageRepository>()

    route("/coffee-shops/product/{id}/image") {

        // Обычный путь: Photoroom вырезает фон, дальше объект ложится на общий холст.
        post {
            ducksTryCatch {
                uploadProductImage(coffeeShopsRepository) { imagesRepository.saveProductImage(it) }
            }
        }

        // Аварийный путь: картинка уже готова и уезжает в хранилище байт в байт.
        // Нужен, когда Photoroom портит конкретный товар — например, срезает часть силуэта
        // или принимает фон за объект, и починить это параметрами запроса не выходит.
        post("/raw") {
            ducksTryCatch {
                uploadProductImage(coffeeShopsRepository) { imagesRepository.saveProductImageAsIs(it) }
            }
        }
    }
}

private suspend fun RoutingContext.uploadProductImage(
    coffeeShopsRepository: AdminCoffeeShopsRepository,
    save: suspend (PartData.FileItem) -> SaveImageResult,
) {
    val productId = call.parameters["id"]?.toLongOrNull()
        ?: throw DucksBadRequestError("Некорректный id товара")

    coffeeShopsRepository.ensureProductExists(productId)

    var result: SaveImageResult? = null

    call.receiveMultipart().forEachPart { part ->
        // Берём только первый файл: несколько картинок на один товар положить некуда,
        // а заливка каждой стоила бы отдельного вызова Photoroom.
        if (result == null && part is PartData.FileItem) {
            result = save(part)
        }

        part.dispose()
    }

    when (val saved = result) {
        null -> call.respond(HttpStatusCode.BadRequest, "В запросе нет файла с картинкой")

        SaveImageResult.UnsupportedFileType ->
            call.respond(HttpStatusCode.UnsupportedMediaType, "Формат файла запрещён")

        is SaveImageResult.Success -> {
            coffeeShopsRepository.setProductImage(productId, saved.imageUrl)

            call.respond(HttpStatusCode.Created, mapOf("url" to saved.imageUrl))
        }
    }
}

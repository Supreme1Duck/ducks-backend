package com.ducks.admin.route

import com.ducks.admin.repository.AdminCoffeeShopsRepository
import com.ducks.admin.request.CreateCoffeeCategoryRequest
import com.ducks.admin.request.CreateCoffeeShopRequest
import com.ducks.admin.request.SetCoffeeShopCoordinatesRequest
import com.ducks.admin.request.SetPinCodeRequest
import com.ducks.admin.response.CreatedCoffeeProductResponse
import com.ducks.auth.admin.JWTAdminPrincipal
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.common.data.SaveImageResult
import com.ducks.features.coffeeshops.seller.data.SellerPinCodeDataSource
import com.ducks.features.coffeeshops.seller.domain.CoffeeShopImageRepository
import com.ducks.util.DucksBadRequestError
import com.ducks.util.ducksJson
import com.ducks.util.ducksTryCatch
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import org.koin.ktor.ext.inject

fun Route.adminCoffeeShopsRoute() {
    val coffeeShopsRepository by application.inject<AdminCoffeeShopsRepository>()
    val pinCodeDataSource by application.inject<SellerPinCodeDataSource>()
    val imagesRepository by application.inject<CoffeeShopImageRepository>()

    route("/coffee-shops") {
        post("/create") {
            ducksTryCatch {
                val request = call.receive<CreateCoffeeShopRequest>()
                val adminId = call.principal<JWTAdminPrincipal>()!!.adminId

                coffeeShopsRepository.createNewShop(request, adminId)

                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/coordinates/set") {
            ducksTryCatch {
                val request = call.receive<SetCoffeeShopCoordinatesRequest>()

                coffeeShopsRepository.setCoordinates(request)

                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/pin/set") {
            ducksTryCatch {
                val request = call.receive<SetPinCodeRequest>()
                pinCodeDataSource.setPin(request.shopId, request.pin)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        /**
         * Заводит товар в кофейне [shopId] по готовой ссылке на картинку. Тело — то же,
         * что шлёт приложение продавца, так что админка переиспользует его форму.
         *
         * imageUrl можно не присылать: товар заведётся без картинки, и её получится
         * долить следом запросом на /coffee-shops/product/{id}/image. Если картинка
         * есть уже сейчас — рядом лежит /create/with-image, он принимает её файлом.
         */
        post("/{shopId}/product/create") {
            ducksTryCatch {
                val shopId = call.parameters["shopId"]?.toLongOrNull()
                    ?: throw DucksBadRequestError("Некорректный id кофейни")

                val request = call.receive<CreateCoffeeProductRequest>()

                val productId = coffeeShopsRepository.createProduct(shopId = shopId, data = request)

                call.respond(HttpStatusCode.Created, mapOf("id" to productId))
            }
        }

        /**
         * То же создание товара, но картинка едет файлом в том же запросе, а не ссылкой.
         *
         * multipart из двух частей: поле `product` с тем же json и сам файл. `imageUrl`
         * в json не нужен — он всё равно перезаписывается ссылкой на залитый файл.
         */
        post("/{shopId}/product/create/with-image") {
            ducksTryCatch {
                createProductWithImage(coffeeShopsRepository, imagesRepository) { bytes, fileName ->
                    imagesRepository.saveProductImage(bytes, fileName)
                }
            }
        }

        // То же, но картинка уезжает в хранилище байт в байт, без Photoroom и холста —
        // для готовых картинок, которым наша обработка только навредит.
        post("/{shopId}/product/create/with-image/raw") {
            ducksTryCatch {
                createProductWithImage(coffeeShopsRepository, imagesRepository) { bytes, fileName ->
                    imagesRepository.saveProductImageAsIs(bytes, fileName)
                }
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

/**
 * Разбирает multipart на json товара и файл, заливает картинку и заводит товар с
 * получившейся ссылкой.
 *
 * Порядок шагов важен: сначала бесплатные проверки, потом заливка. Иначе опечатка
 * в id стоила бы вызова Photoroom, а в хранилище оставался бы файл без товара.
 */
private suspend fun RoutingContext.createProductWithImage(
    coffeeShopsRepository: AdminCoffeeShopsRepository,
    imagesRepository: CoffeeShopImageRepository,
    saveImage: suspend (imageBytes: ByteArray, originalFileName: String?) -> SaveImageResult,
) {
    val shopId = call.parameters["shopId"]?.toLongOrNull()
        ?: throw DucksBadRequestError("Некорректный id кофейни")

    var productJson: String? = null
    var imageBytes: ByteArray? = null
    var imageFileName: String? = null

    call.receiveMultipart().forEachPart { part ->
        when {
            part is PartData.FormItem && part.name == PRODUCT_PART_NAME ->
                productJson = part.value

            // Файл нельзя отложить на потом: как только разбор дошёл до следующей части,
            // поток предыдущей уже закрыт. Берём первый, остальные игнорируем —
            // картинка у товара одна.
            part is PartData.FileItem && imageBytes == null -> {
                imageBytes = imagesRepository.readImageBytes(part)
                imageFileName = part.originalFileName
            }
        }

        part.dispose()
    }

    val json = productJson
        ?: throw DucksBadRequestError("В запросе нет поля $PRODUCT_PART_NAME с json товара")
    val image = imageBytes
        ?: throw DucksBadRequestError("В запросе нет файла с картинкой")

    val request = try {
        ducksJson.decodeFromString<CreateCoffeeProductRequest>(json)
    } catch (error: SerializationException) {
        throw DucksBadRequestError("Не удалось разобрать json товара: ${error.message}")
    }

    coffeeShopsRepository.ensureShopAndCategoryExist(shopId = shopId, categoryId = request.categoryId)

    when (val saved = saveImage(image, imageFileName)) {
        SaveImageResult.UnsupportedFileType ->
            call.respond(HttpStatusCode.UnsupportedMediaType, "Формат файла запрещён")

        is SaveImageResult.Success -> {
            val productId = coffeeShopsRepository.createProduct(
                shopId = shopId,
                data = request.copy(imageUrl = saved.imageUrl),
            )

            call.respond(
                HttpStatusCode.Created,
                CreatedCoffeeProductResponse(id = productId, imageUrl = saved.imageUrl),
            )
        }
    }
}

/** Имя части multipart с json товара. */
private const val PRODUCT_PART_NAME = "product"

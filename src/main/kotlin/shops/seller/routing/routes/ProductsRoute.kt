package com.ducks.shops.seller.routing.routes

import com.ducks.shops.common.model.ShopProductModel
import com.ducks.shops.seller.analytics.SellersAnalytics
import com.ducks.shops.seller.data.ShopImageRepository
import com.ducks.shops.seller.data.model.DeleteImageResult
import com.ducks.shops.seller.data.model.SaveImageResult
import com.ducks.shops.seller.domain.ShopProductInteractor
import com.ducks.shops.seller.getSellerPrincipal
import com.ducks.shops.seller.routing.request.CreateProductRequest
import com.ducks.shops.seller.routing.request.DeleteProductRequest
import com.ducks.shops.seller.routing.request.UpdateProductRequest
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.productsRoute() {
    val shopProductInteractor by inject<ShopProductInteractor>()
    val shopImageRepository by inject<ShopImageRepository>()
    val sellersAnalytics by inject<SellersAnalytics>()

    post("/create/product") {
        try {
            val product = call.receive<CreateProductRequest>()
            val shopId = getSellerPrincipal().shopId

            shopProductInteractor.insert(
                shopId = shopId,
                sizeIds = product.sizeIds,
                categoryId = product.categoryId,
                productModel = ShopProductModel(
                    name = product.name,
                    description = product.description,
                    shopId = shopId,
                    brandName = product.brandName,
                    imageUrls = product.imageUrls,
                    price = product.price,
                )
            )

            call.respond(HttpStatusCode.Created)
        } catch (e: Exception) {
            sellersAnalytics.logException(e)
            call.respond(HttpStatusCode.InternalServerError, message = "Выбранного магазина не существует")
        }
    }

    post("/update/product") {
        try {
            val product = call.receive<UpdateProductRequest>()
            val shopId = getSellerPrincipal().shopId

            shopProductInteractor.update(
                shopId = shopId,
                productId = product.id,
                updateParametersMap = product.updateMap,
            )

            call.respond(HttpStatusCode.Created)
        } catch (e: Exception) {
            sellersAnalytics.logException(e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    delete("/delete/product") {
        try {
            val request = call.receive<DeleteProductRequest>()
            val shopId = getSellerPrincipal().shopId

            shopProductInteractor.delete(productId = request.productId, shopId = shopId)
        } catch (e: Exception) {
            sellersAnalytics.logException(e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/upload/image") {
        val multipart = call.receiveMultipart()

        try {
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val result = shopImageRepository.saveImage(part)

                    when (result) {
                        SaveImageResult.UnsupportedFileType -> {
                            call.respond(HttpStatusCode.UnsupportedMediaType, "Формат файла запрещён")
                        }

                        is SaveImageResult.Success -> {
                            call.respond(mapOf("url" to result.imageUrl))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            sellersAnalytics.logException(e)
            call.respond(HttpStatusCode.InternalServerError, "Ошибка в процессе сохранения -> $e")
        }
    }

    delete("/delete/image/{imageUrl}") {
        try {
            val shopId = getSellerPrincipal().shopId
            val imageUrl = call.parameters["imageUrl"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "Необходимо передать имя файла")
                return@delete
            }

            val result = shopImageRepository.deleteImage(shopId, imageUrl)

            when (result) {
                DeleteImageResult.FileNotFound -> {
                    call.respond(HttpStatusCode.NotFound, "Файл не найден")
                }

                DeleteImageResult.InternalError -> {
                    call.respond(HttpStatusCode.InternalServerError, "Удалить не получилось")
                }

                DeleteImageResult.UnsupportedImageType -> {
                    call.respond(HttpStatusCode.UnsupportedMediaType, "Формат файла запрещён")
                }

                DeleteImageResult.Success -> {
                    call.respond(HttpStatusCode.NoContent)
                }

                DeleteImageResult.TryToDeleteAlienFile -> {
                    sellersAnalytics.logSuspiciousDeleteFile(shopId = shopId, fileName = imageUrl)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        } catch (e: Exception) {
            sellersAnalytics.logException(e)
            call.respond(HttpStatusCode.InternalServerError, e.printStackTrace())
        }
    }
}
package com.ducks.coffeeshops.seller.routings

import com.ducks.coffeeshops.seller.domain.CoffeeShopImageRepository
import com.ducks.coffeeshops.seller.domain.SellerCoffeeProductRepository
import com.ducks.coffeeshops.seller.domain.SellerCoffeeShopRepository
import com.ducks.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.coffeeshops.seller.routings.request.*
import com.ducks.common.data.SaveImageResult
import com.ducks.shops.seller.getSellerPrincipal
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.shopsAndProductsRoute() {
    val coffeeShopsRepository by application.inject<SellerCoffeeShopRepository>()
    val coffeeProductsRepository by application.inject<SellerCoffeeProductRepository>()
    val coffeeImagesRepository by application.inject<CoffeeShopImageRepository>()

    post("/shop/update") {
        try {
            val request = call.receive<UpdateCoffeeShopRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            if (principalShopId != request.shopId) {
                throw IllegalStateException("Попытка обновить с чужим токеном")
            }

            coffeeShopsRepository.updateShop(
                shopId = request.shopId,
                updateMap = request.updateMap
            )

            call.respond(HttpStatusCode.NoContent)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/shops/schedule") {
        try {
            val shopId = getSellerPrincipal().shopId
            val request = call.receive<SetCoffeeShopScheduleRequest>()

            coffeeShopsRepository.setSchedule(shopId, request)

            call.respond(HttpStatusCode.Created)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/product/create") {
        try {
            val request = call.receive<CreateCoffeeProductRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeProductsRepository.insert(
                shopId = principalShopId,
                data = request
            )

            call.respond(HttpStatusCode.Created)
        } catch (e: Exception) {
            println("$e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/product/update") {
        try {
            val request = call.receive<UpdateCoffeeProductRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeProductsRepository.update(shopId = principalShopId, data = request)

            call.respond(HttpStatusCode.NoContent)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    delete("/product/delete") {
        try {
            val request = call.receive<DeleteCoffeeProductRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeProductsRepository.delete(shopId = principalShopId, productId = request.productId)

            call.respond(HttpStatusCode.NoContent)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/image/upload") {
        try {
            getCoffeeShopSellerPrincipal()

            val request = call.receiveMultipart()

            request.forEachPart {
                if (it is PartData.FileItem) {
                    val result = coffeeImagesRepository.saveImage(it)

                    when (result) {
                        SaveImageResult.UnsupportedFileType -> {
                            call.respond(HttpStatusCode.UnsupportedMediaType, "Формат файла запрещён")
                        }

                        is SaveImageResult.Success -> {
                            call.respond(HttpStatusCode.Created, mapOf("url" to result.imageUrl))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("/product/upload/image") {
        try {
            getCoffeeShopSellerPrincipal()

            val request = call.receiveMultipart()

            request.forEachPart {
                if (it is PartData.FileItem) {
                    val result = coffeeImagesRepository.saveProductImage(it)

                    when (result) {
                        SaveImageResult.UnsupportedFileType -> {
                            call.respond(HttpStatusCode.UnsupportedMediaType, "Формат файла запрещён")
                        }

                        is SaveImageResult.Success -> {
                            call.respond(HttpStatusCode.Created, mapOf("url" to result.imageUrl))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
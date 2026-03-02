package com.ducks.features.coffeeshops.seller.routings

import com.ducks.common.data.SaveImageResult
import com.ducks.features.coffeeshops.seller.domain.CoffeeShopImageRepository
import com.ducks.features.coffeeshops.seller.domain.SellerCoffeeProductRepository
import com.ducks.features.coffeeshops.seller.domain.SellerCoffeeShopRepository
import com.ducks.features.coffeeshops.seller.getCoffeeShopSellerPrincipal
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.DeleteCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.shop.SetCoffeeShopScheduleRequest
import com.ducks.features.coffeeshops.seller.routings.request.shop.SetTechnicalPauseRequest
import com.ducks.features.coffeeshops.seller.routings.request.shop.UpdateCoffeeShopRequest
import com.ducks.util.ducksTryCatch
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

    get("/shop/details") {
        ducksTryCatch {
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            call.respond(coffeeShopsRepository.getShopDetails(principalShopId))
        }
    }

    get("/shop/products") {
        ducksTryCatch {
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            call.respond(coffeeProductsRepository.fetchProductsByShop(principalShopId))
        }
    }

    get("/products/{id}") {
        ducksTryCatch {
            val productId = call.parameters["productId"]!!.toLong()

            val product = coffeeProductsRepository.getProductDetails(productId)

            call.respond(HttpStatusCode.OK, product)
        }
    }

    post("/shop/update") {
        ducksTryCatch {
            val request = call.receive<UpdateCoffeeShopRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeShopsRepository.updateShop(principalShopId, request)

            call.respond(HttpStatusCode.NoContent)
        }
    }

    post("/shop/schedule") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val request = call.receive<SetCoffeeShopScheduleRequest>()

            coffeeShopsRepository.setSchedule(shopId, request)

            call.respond(HttpStatusCode.Created)
        }
    }

    post("/shop/technical-pause") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId
            val request = call.receive<SetTechnicalPauseRequest>()

            coffeeShopsRepository.addTechnicalPause(
                shopId = shopId,
                startsAt = request.startsAt,
                endsAt = request.endsAt
            )

            call.respond(HttpStatusCode.Created)
        }
    }

    delete("/shop/technical-pause") {
        ducksTryCatch {
            val shopId = getCoffeeShopSellerPrincipal().shopId

            coffeeShopsRepository.deletePause(shopId = shopId)

            call.respond(HttpStatusCode.Created)
        }
    }

    post("/product/create") {
        ducksTryCatch {
            val request = call.receive<CreateCoffeeProductRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeProductsRepository.insert(
                shopId = principalShopId,
                data = request
            )

            call.respond(HttpStatusCode.Created)
        }
    }

    post("/product/update") {
        ducksTryCatch {
            val request = call.receive<UpdateCoffeeProductRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeProductsRepository.update(shopId = principalShopId, data = request)

            call.respond(HttpStatusCode.NoContent)
        }
    }

    delete("/product/delete") {
        ducksTryCatch {
            val request = call.receive<DeleteCoffeeProductRequest>()
            val principalShopId = getCoffeeShopSellerPrincipal().shopId

            coffeeProductsRepository.delete(shopId = principalShopId, productId = request.productId)

            call.respond(HttpStatusCode.NoContent)
        }
    }

    post("/image/upload") {
        ducksTryCatch {
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
        }
    }

    post("/product/upload/image") {
        ducksTryCatch {
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
        }
    }
}
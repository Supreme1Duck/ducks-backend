package com.ducks.service

import com.ducks.features.coffeeshops.service.ActualizeCoffeeShopsVisibilityService
import com.ducks.features.coffeeshops.service.ProductRecommendationsService
import com.ducks.features.orders.service.ActualizeOrdersService
import com.ducks.features.orders.service.ActualizeTechnicalPausesService
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.service.AnonymizeDeletedUsersService
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun Application.runServices() {

    val actualizeOrdersService by this.inject<ActualizeOrdersService>()
    val actualizeTechnicalPausesService by this.inject<ActualizeTechnicalPausesService>()
    val calculateCoffeeShopsOrdersTimeService by this.inject<CalculateCoffeeShopsOrdersTimeService>()
    val coffeeShopDeleteUnusedImagesService by this.inject<CoffeeShopDeleteUnusedImagesService>()
    val coffeeShopNormalizeProductImagesService by this.inject<CoffeeShopNormalizeProductImagesService>()
    val actualizeCoffeeShopsVisibilityService by this.inject<ActualizeCoffeeShopsVisibilityService>()
    val anonymizeDeletedUsersService by this.inject<AnonymizeDeletedUsersService>()
    val productRecommendationsService by this.inject<ProductRecommendationsService>()

    actualizeOrdersService.invoke()
    actualizeTechnicalPausesService.invoke()
    calculateCoffeeShopsOrdersTimeService.initialize()
    coffeeShopDeleteUnusedImagesService.invoke()

    actualizeCoffeeShopsVisibilityService.initialize()
    anonymizeDeletedUsersService.invoke()
    productRecommendationsService.invoke()

    // Нормализация тянет картинки обычным HTTP-запросом, и у части товаров ссылка ведёт
    // на этот же сервер: пока не переехали в S3, они лежат на диске и раздаются staticFiles.
    // runServices() отрабатывает внутри module(), то есть до того, как Ktor поднимет
    // коннекторы, — запущенный отсюда сервис стучался бы в собственный закрытый порт
    // и ловил Connection refused на каждом старте. ServerReady наступает уже после бинда.
    monitor.subscribe(ServerReady) {
        coffeeShopNormalizeProductImagesService.invoke()
    }
}
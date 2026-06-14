package com.ducks.service

import com.ducks.features.coffeeshops.service.ActualizeCoffeeShopsVisibilityService
import com.ducks.features.orders.service.ActualizeOrdersService
import com.ducks.features.orders.service.ActualizeTechnicalPausesService
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun Application.runServices() {

    val actualizeOrdersService by this.inject<ActualizeOrdersService>()
    val actualizeTechnicalPausesService by this.inject<ActualizeTechnicalPausesService>()
    val calculateCoffeeShopsOrdersTimeService by this.inject<CalculateCoffeeShopsOrdersTimeService>()
    val coffeeShopDeleteUnusedImagesService by this.inject<CoffeeShopDeleteUnusedImagesService>()
    val actualizeCoffeeShopsVisibilityService by this.inject<ActualizeCoffeeShopsVisibilityService>()

    actualizeOrdersService.invoke()
    actualizeTechnicalPausesService.invoke()
    calculateCoffeeShopsOrdersTimeService.initialize()
    coffeeShopDeleteUnusedImagesService.invoke()
    actualizeCoffeeShopsVisibilityService.initialize()
}
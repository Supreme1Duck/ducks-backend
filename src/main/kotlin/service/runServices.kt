package com.ducks.service

import com.ducks.features.orders.service.ActualizeOrdersService
import com.ducks.features.orders.service.ActualizeTechnicalPausesService
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun Application.runServices() {

    val actualizeOrdersService by this.inject<ActualizeOrdersService>()
    val actualizeTechnicalPausesService by this.inject<ActualizeTechnicalPausesService>()

    actualizeOrdersService.invoke()
    actualizeTechnicalPausesService.invoke()
}
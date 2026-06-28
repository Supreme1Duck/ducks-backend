package com.ducks.features.orders

import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import com.ducks.features.orders.service.ActualizeOrdersService
import com.ducks.features.orders.service.ActualizeTechnicalPausesService
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import org.koin.dsl.module

val ordersModule = module {

    single { ActualizeOrdersService(get(), get()) }
    single { CalculateCoffeeShopsOrdersTimeService(get(), get()) }
    single { FetchAvailableOrdersTimeListRepository() }
    single { ActualizeTechnicalPausesService(get()) }
}
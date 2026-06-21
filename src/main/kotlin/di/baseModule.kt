package com.ducks.di

import com.ducks.admin.adminsModule
import com.ducks.features.coffeeshops.coffeeShopsModule
import com.ducks.features.orders.ordersModule
import com.ducks.features.user.usersModule
import com.ducks.features.shops.shopsModule
import com.ducks.service.serviceModule
import org.koin.dsl.module

fun baseModule(baseUrl: String) = module {
    includes(adminsModule, usersModule, shopsModule, coffeeShopsModule(baseUrl), ordersModule, serviceModule)
}
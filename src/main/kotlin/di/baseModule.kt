package com.ducks.di

import com.ducks.admin.adminsModule
import com.ducks.features.coffeeshops.coffeeShopsModule
import com.ducks.features.user.usersModule
import com.ducks.features.shops.shopsModule
import org.koin.dsl.module

fun baseModule() = module {
    includes(adminsModule, usersModule, shopsModule, coffeeShopsModule)
}
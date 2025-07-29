package com.ducks.di

import com.ducks.admin.adminsModule
import com.ducks.coffeeshops.coffeeShopsModule
import com.ducks.shops.shopsModule
import org.koin.dsl.module

fun baseModule() = module {
    includes(adminsModule, userModule, shopsModule, coffeeShopsModule)
}
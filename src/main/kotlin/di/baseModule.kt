package com.ducks.di

import com.ducks.admin.adminsModule
import com.ducks.shops.shopsModule
import org.koin.dsl.module

fun baseModule() = module {
    includes(userModule, shopsModule, adminsModule)
}
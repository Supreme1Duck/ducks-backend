package com.ducks.di

import com.ducks.service.JwtService
import org.koin.dsl.module

fun baseModule() = module {
    includes(userModule())
    includes(shopsModule())

    single { JwtService(get(), get()) }
}
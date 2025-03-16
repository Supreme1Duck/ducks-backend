package com.ducks.di

import com.ducks.service.JwtService
import org.koin.dsl.module

fun baseModule() = module {
    includes(userModule())

    single { JwtService(get(), get()) }
}
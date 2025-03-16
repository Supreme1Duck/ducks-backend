package com.ducks.di

import com.ducks.repository.UserRepository
import org.koin.dsl.module

fun userModule() = module {
    single { UserRepository() }
}
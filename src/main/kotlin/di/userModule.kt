package com.ducks.di

import com.ducks.repository.UserRepository
import org.koin.dsl.module

val userModule = module {
    single { UserRepository() }
}
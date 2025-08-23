package com.ducks.features.user

import com.ducks.features.user.data.UsersRepository
import org.koin.dsl.module

val usersModule = module {

    single { UsersRepository() }

}
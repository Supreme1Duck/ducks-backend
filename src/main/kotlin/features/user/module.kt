package com.ducks.features.user

import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.domain.ClientCreateOrdersRepository
import com.ducks.features.user.domain.ClientsOrdersRepository
import org.koin.dsl.module

val usersModule = module {

    single { UsersRepository() }

    // orders
    single { ClientsOrdersRepository(get()) }
    single { ClientCreateOrdersRepository(get()) }
}
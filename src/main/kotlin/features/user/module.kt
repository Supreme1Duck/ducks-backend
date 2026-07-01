package com.ducks.features.user

import com.ducks.features.user.data.UsersRepository
import com.ducks.features.user.domain.ClientCreateOrdersRepository
import com.ducks.features.user.domain.ClientsOrdersRepository
import com.ducks.features.user.domain.ReorderPreviewRepository
import com.ducks.features.user.ratelimit.OtpRateLimiter
import org.koin.dsl.module

val usersModule = module {

    single { UsersRepository() }
    single { OtpRateLimiter() }

    // orders
    single { ClientsOrdersRepository(get()) }
    single { ClientCreateOrdersRepository(get()) }
    single { ReorderPreviewRepository() }
}
package com.ducks.features.landing

import com.ducks.features.landing.domain.CallbackRequestService
import com.ducks.features.landing.ratelimit.CallbackRateLimiter
import org.koin.dsl.module

val landingModule = module {
    single { CallbackRateLimiter() }
    single { CallbackRequestService(get()) }
}

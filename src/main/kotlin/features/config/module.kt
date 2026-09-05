package com.ducks.features.config

import com.ducks.features.config.data.ClientConfigDataSource
import com.ducks.features.config.domain.ClientConfigRepository
import org.koin.dsl.module

val clientConfigModule = module {
    single { ClientConfigDataSource() }
    single { ClientConfigRepository(get()) }
}

package com.ducks.admin

import com.ducks.admin.analytics.AdminAnalytics
import com.ducks.admin.api.CoffeeShopCredentialsRepository
import com.ducks.admin.api.ShopCredentialsRepository
import com.ducks.admin.repository.AdminCoffeeShopsRepository
import com.ducks.admin.repository.AdminRepository
import com.ducks.admin.repository.AdminShopsRepository
import com.ducks.auth.admin.JWTAdminService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.koin.dsl.module

val adminsModule = module {
    single { AdminRepository() }
    single { JWTAdminService(get(), get()) }
    single { AdminAnalytics() }

    // Shops
    single { AdminShopsRepository() }
    single { ShopCredentialsRepository() }

    // Coffee-shops
    single { AdminCoffeeShopsRepository(get(), get(), get()) }
    single { CoffeeShopCredentialsRepository() }

    single<HttpClient> { HttpClient(CIO) }
}
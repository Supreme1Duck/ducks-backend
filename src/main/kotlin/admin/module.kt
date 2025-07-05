package com.ducks.admin

import com.ducks.admin.analytics.AdminAnalytics
import com.ducks.admin.api.ShopCredentialsRepository
import com.ducks.admin.repository.AdminRepository
import com.ducks.admin.repository.AdminShopsRepository
import com.ducks.auth.admin.JWTAdminService
import org.koin.dsl.module

val adminsModule = module {
    single { AdminRepository() }
    single { JWTAdminService(get(), get()) }
    single { AdminAnalytics() }
    single { AdminShopsRepository() }
    single { ShopCredentialsRepository() }
}
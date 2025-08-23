package com.ducks.features.shops.seller.di

import com.ducks.features.shops.seller.analytics.SellersAnalytics
import com.ducks.features.shops.seller.domain.ShopImageRepository
import org.koin.dsl.module

val sellerModule = module {
    single { SellersAnalytics() }
    single { ShopImageRepository() }
}
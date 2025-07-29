package com.ducks.shops.seller.di

import com.ducks.shops.seller.analytics.SellersAnalytics
import com.ducks.shops.seller.domain.ShopImageRepository
import org.koin.dsl.module

val sellerModule = module {
    single { SellersAnalytics() }
    single { ShopImageRepository() }
}
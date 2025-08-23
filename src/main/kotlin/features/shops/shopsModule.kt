package com.ducks.features.shops

import com.ducks.auth.client.JWTClientService
import com.ducks.auth.seller.JWTSellerService
import com.ducks.features.shops.common.data.ShopDataSource
import com.ducks.features.shops.common.data.ShopProductsDataSource
import com.ducks.features.shops.common.repository.*
import com.ducks.features.shops.seller.domain.InsertShopProductColorUseCase
import com.ducks.features.shops.seller.domain.ShopProductInteractor
import com.ducks.features.shops.seller.di.sellerModule
import com.ducks.features.shops.seller.domain.ShopImageRepository
import org.koin.dsl.module

val shopsModule = module {
    // JWT
    single { JWTClientService(get(), get()) }
    single { JWTSellerService(get(), get()) }

    // Repository
    single { ShopsRepository(get()) }
    single { ShopProductsRepository(get(), get()) }
    single { ShopProductCategoryRepository() }
    single { ShopProductColorsRepository() }
    single { ShopProductSizesRepository() }
    single { ShopProductsWithSizesRepository() }
    single { ShopImageRepository() }

    // Data sources
    single { ShopDataSource(get()) }
    single { ShopProductsDataSource() }

    // Use case
    single { ShopProductInteractor(get(), get()) }
    single { InsertShopProductColorUseCase() }

    includes(sellerModule)
}
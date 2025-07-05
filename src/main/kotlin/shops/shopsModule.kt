package com.ducks.shops

import com.ducks.auth.client.JWTClientService
import com.ducks.auth.seller.JWTSellerService
import com.ducks.shops.common.data.ShopDataSource
import com.ducks.shops.common.data.ShopProductsDataSource
import com.ducks.shops.seller.domain.InsertShopProductColorUseCase
import com.ducks.shops.seller.domain.InsertOrUpdateShopProductUseCase
import com.ducks.shops.common.repository.*
import com.ducks.shops.seller.di.sellerModule
import org.koin.dsl.module

val shopsModule = module {
    // JWT
    single { JWTClientService(get(), get()) }
    single { JWTSellerService(get(), get()) }

    // Repository
    single { ShopsRepository(get()) }
    single { ShopProductsRepository(get()) }
    single { ShopProductCategoryRepository() }
    single { ShopProductColorsRepository() }
    single { ShopProductSizesRepository() }
    single { ShopProductsWithSizesRepository() }

    // Data sources
    single { ShopDataSource(get()) }
    single { ShopProductsDataSource() }

    // Use case
    single { InsertOrUpdateShopProductUseCase(get(), get()) }
    single { InsertShopProductColorUseCase() }

    includes(sellerModule)
}
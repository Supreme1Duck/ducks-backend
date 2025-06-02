package com.ducks.di

import com.ducks.domain.InsertShopProductColorUseCase
import com.ducks.domain.InsertShopProductUseCase
import com.ducks.domain.SearchProductsUseCase
import com.ducks.repository.*
import org.koin.dsl.module

fun shopsModule() = module {
    // Repository
    single { ShopsRepository() }
    single { ShopProductsRepository() }
    single { ShopProductCategoryRepository() }
    single { ShopProductColorsRepository() }
    single { ShopProductSizesRepository() }
    single { ShopProductsWithSizesRepository() }

    // Use case
    single { InsertShopProductUseCase(get(), get()) }
    single { InsertShopProductColorUseCase() }
    single { SearchProductsUseCase() }
}
package com.ducks.di

import com.ducks.database.repository.ShopProductCategoryRepository
import com.ducks.database.repository.ShopProductsRepository
import com.ducks.database.repository.ShopsRepository
import org.koin.dsl.module

fun shopsModule() = module {
    single { ShopsRepository() }
    single { ShopProductsRepository() }
    single { ShopProductCategoryRepository() }
}
package com.ducks.features.coffeeshops

import com.ducks.auth.coffee_seller.JWTCoffeeSellerService
import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.CoffeeShopsDataSource
import com.ducks.features.coffeeshops.client.domain.CoffeeProductsRepository
import com.ducks.features.coffeeshops.client.domain.CoffeeShopsRepository
import com.ducks.features.coffeeshops.seller.analytics.CoffeeSellerAnalyticsRepository
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeCategoriesRepository
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeConstructorsDataSource
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeProductDataSource
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeShopsDataSource
import com.ducks.features.coffeeshops.seller.data.SellerPinCodeDataSource
import com.ducks.features.coffeeshops.seller.domain.*
import com.ducks.features.coffeeshops.service.ActualizeCoffeeShopsVisibilityService
import org.koin.dsl.module

fun coffeeShopsModule(baseUrl: String) = module {

    single { CoffeeShopsRepository(get(), get(), get()) }
    single { CoffeeProductsRepository(get()) }

    single { CoffeeShopsDataSource() }
    single { CoffeeProductsDataSource() }
    single { CoffeeShopImageRepository(get(), baseUrl) }

    single { SellerCoffeeProductDataSource() }
    single { SellerCoffeeShopsDataSource() }
    single { SellerCoffeeCategoriesRepository() }

    single { SellerCoffeeShopRepository(get(), get(), get(), get()) }
    single { SellerCoffeeProductRepository(get(), get()) }

    single { SellerCoffeeConstructorsDataSource() }
    single { SellerConstructorsRepository(get()) }

    single { JWTCoffeeSellerService(get(), get()) }

    single { ObserveOrdersRepository() }
    single { SellerOrdersRepository(get()) }
    single { CoffeeSellerAnalyticsRepository() }
    single { SellerPinCodeDataSource() }
    single { ActualizeCoffeeShopsVisibilityService(get()) }
}
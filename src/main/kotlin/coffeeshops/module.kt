package com.ducks.coffeeshops

import com.ducks.auth.coffee_seller.JWTCoffeeSellerService
import com.ducks.coffeeshops.common.data.CoffeeProductsDataSource
import com.ducks.coffeeshops.common.data.CoffeeShopsDataSource
import com.ducks.coffeeshops.common.domain.CoffeeProductsRepository
import com.ducks.coffeeshops.common.domain.CoffeeShopsRepository
import com.ducks.coffeeshops.seller.data.SellerCoffeeCategoriesRepository
import com.ducks.coffeeshops.seller.data.SellerCoffeeConstructorsDataSource
import com.ducks.coffeeshops.seller.data.SellerCoffeeProductDataSource
import com.ducks.coffeeshops.seller.data.SellerCoffeeShopsDataSource
import com.ducks.coffeeshops.seller.domain.CoffeeShopImageRepository
import com.ducks.coffeeshops.seller.domain.SellerCoffeeProductRepository
import com.ducks.coffeeshops.seller.domain.SellerCoffeeShopRepository
import com.ducks.coffeeshops.seller.domain.SellerConstructorsRepository
import org.koin.dsl.module

val coffeeShopsModule = module {

    single { CoffeeShopsRepository(get(), get()) }
    single { CoffeeProductsRepository(get()) }

    single { CoffeeShopsDataSource() }
    single { CoffeeProductsDataSource() }
    single { CoffeeShopImageRepository() }

    single { SellerCoffeeProductDataSource() }
    single { SellerCoffeeShopsDataSource() }
    single { SellerCoffeeCategoriesRepository() }

    single { SellerCoffeeShopRepository(get(), get()) }
    single { SellerCoffeeProductRepository(get(), get()) }

    single { SellerCoffeeConstructorsDataSource() }
    single { SellerConstructorsRepository(get()) }

    single { JWTCoffeeSellerService(get(), get()) }
}
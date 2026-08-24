package com.ducks.service

import org.koin.dsl.module

val serviceModule = module {
    single { MinuteChangeNotifierService() }
    single { CoffeeShopDeleteUnusedImagesService(get()) }
    single { CoffeeShopNormalizeProductImagesService(get()) }
    single { PushNotificationService() }
}
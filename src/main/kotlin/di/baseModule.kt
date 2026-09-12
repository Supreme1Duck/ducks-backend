package com.ducks.di

import com.ducks.admin.adminsModule
import com.ducks.common.storage.S3Config
import com.ducks.features.coffeeshops.coffeeShopsModule
import com.ducks.features.config.clientConfigModule
import com.ducks.features.landing.landingModule
import com.ducks.features.orders.ordersModule
import com.ducks.features.sms.SmsByConfig
import com.ducks.features.sms.smsModule
import com.ducks.features.telegram.TelegramConfig
import com.ducks.features.telegram.telegramModule
import com.ducks.features.user.usersModule
import com.ducks.features.shops.shopsModule
import com.ducks.service.serviceModule
import org.koin.dsl.module

fun baseModule(
    photoroomApiKey: String,
    s3Config: S3Config,
    smsByConfig: SmsByConfig,
    telegramConfig: TelegramConfig,
) = module {
    includes(
        adminsModule,
        clientConfigModule,
        usersModule,
        shopsModule,
        coffeeShopsModule(photoroomApiKey, s3Config),
        ordersModule,
        smsModule(smsByConfig),
        telegramModule(telegramConfig),
        landingModule,
        serviceModule
    )
}
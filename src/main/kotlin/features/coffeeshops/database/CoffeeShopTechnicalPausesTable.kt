package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeShopTechnicalPausesTable : LongIdTable("ducks_coffee_shop_technical_pause_table") {

    val startsAt = long("starts_at")
    val endsAt = long("ends_at")

    val coffeeShop = reference("coffeeShop", CoffeeShopTable)

    val isActive = bool("isActive").default(true)
}
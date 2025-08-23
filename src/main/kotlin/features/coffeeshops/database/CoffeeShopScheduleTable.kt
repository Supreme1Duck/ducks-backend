package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeShopScheduleTable: LongIdTable("ducks_coffee_shop_schedule_table") {

    val shopId = reference("shop_id", CoffeeShopTable)
    val dayOfWeek = integer("dayOfWeek")
    val startTime = text("startTime").nullable()
    val endTime = text("endTime").nullable()
    val isClosed = bool("isClosed").default(false)
}
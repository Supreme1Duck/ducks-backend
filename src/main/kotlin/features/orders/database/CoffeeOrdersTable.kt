package com.ducks.features.orders.database

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.user.database.UserTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeOrdersTable : LongIdTable("ducks_coffee_orders_table") {

    val coffeeShop = reference("coffee_shop_id", CoffeeShopTable)
    val userId = reference("user_id", UserTable)

    val createdTime = long("created_timestamp")
    val acceptedTime = long("accepted_timestamp").nullable()
    val finishedTime = long("finished_time").nullable()
    val isCancelledBySeller = bool("isCancelledBySeller").default(false)
    val cancelledMessage = text("cancelled_message").nullable()

    val isExpired = bool("isExpired").default(false)
    val isCancelledByClient = bool("isCancelledByClient").default(false)

    val comment = text("comment").nullable()

    val price = decimal("price", precision = 15, scale = 2)
    val tips = decimal("tips", precision = 15, scale = 2).nullable()
    val totalPrice = decimal("total_price", precision = 15, scale = 2)

    val isToTime = bool("is_to_time").default(false)

    val timeToCookInMinutes = integer("time_to_cook_in_minutes")
    val estimatedFinishTime = long("estimated_finish_time").nullable()
}
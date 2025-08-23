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
    val cancelledBySellerTime = long("cancelled_by_seller_time").nullable()
    val cancelledByClientTime = long("cancelled_by_client_time").nullable()
    val cancelledMessage = text("cancelled_message").nullable()

    val comment = text("comment").nullable()

    val estimatedFinishTime = long("estimated_finish_time").nullable()
}
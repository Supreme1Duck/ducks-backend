package com.ducks.features.coffeeshops.database

import com.ducks.util.StringListSerializer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.jsonb
import util.citext

object CoffeeShopTable : LongIdTable("ducks_coffee_shop_table") {

    val name = citext("name", length = 100)
    val address = citext("address", length = 100)
    val description = text("description").nullable()

    val isShown = bool("isShown").default(false)

    val tags = jsonb(
        name = "tags",
        serialize = StringListSerializer::serialize,
        deserialize = StringListSerializer::deserialize,
    ).nullable()

    val imageUrls = jsonb(
        name = "photoUrls",
        serialize = StringListSerializer::serialize,
        deserialize = StringListSerializer::deserialize,
    ).nullable()

    val isTemporaryClosed = bool("isTemporaryClosed").default(false)

    // если null - то в кофейне нет свободного времени для заказа
    val closestTimeToTakeOrders = long("closest_time_to_take_orders").nullable()

    // По умолчанию для всех продуктов.
    val secondsToCook = integer("seconds_to_cook").default(120)

    val seatsCapacity = integer("seatsCapacity").default(10)
    val lowestPrice = integer("lowestPrice").nullable()
}
package com.ducks.features.coffeeshops.database

import com.ducks.util.StringListSerializer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.jsonb
import util.citext

object CoffeeShopTable: LongIdTable("ducks_coffee_shop_table") {

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
    val closestTimeToCookInMinutes = integer("closest_time_to_cook_in_minutes").default(5)
    val secondsToCook = integer("seconds_to_cook").default(120)

    val seatsCapacity = integer("seatsCapacity").default(10)
    val lowestPrice = integer("lowestPrice").nullable()
}
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
    val temporaryClosedReason = text("temporaryClosedReason").nullable()

    // По умолчанию для всех продуктов.
    val minutesToCook = integer("minutes_to_cook").default(2)

    val tablesCapacity = integer("tablesCapacity").default(10)
    val freeTables = integer("freeTables").default(10)

    val lowestPrice = integer("lowestPrice").nullable()

    // если null - то в кофейне нет свободного времени для заказа
    val closestTimeToTakeOrders = long("closest_time_to_take_orders").nullable()

    // 0 - принимает заказы
    // 1 - только короткий заказ
    // 2 - вне времени работы заведения
    // 3 - Очередь заказов в сумме больше 1 часа
    val canTakeOrdersReason = integer("can_take_orders_reason").nullable()

    val rating = double("rating")
}
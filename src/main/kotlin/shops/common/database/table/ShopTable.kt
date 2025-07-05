package com.ducks.shops.common.database.table

import com.ducks.util.StringListSerializer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.jsonb
import util.citext

object ShopTable : LongIdTable("ducks_shop_table") {
    val name = citext("name", length = 50)
    val description = text("description").nullable()
    val address = citext("address", length = 100)
    val photoUrls = jsonb(
        name = "photoUrls",
        serialize = StringListSerializer::serialize,
        deserialize = StringListSerializer::deserialize
    )
}
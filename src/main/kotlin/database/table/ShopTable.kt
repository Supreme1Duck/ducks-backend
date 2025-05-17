package com.ducks.database.table

import com.ducks.util.StringListSerializer
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.jsonb

object ShopTable : LongIdTable("ducks_shop_table") {
    val name = varchar("name", length = 50)
    val description = text("description").nullable()
    val address = varchar("address", length = 100)
    val photoUrls = jsonb(
        name = "photoUrls",
        serialize = StringListSerializer::serialize,
        deserialize = StringListSerializer::deserialize
    )
}
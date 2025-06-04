package com.ducks.database.table

import com.ducks.util.StringListSerializer
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.json.jsonb
import util.citext

object ShopProductTable : IdTable<Long>("ducks_shop_product_table") {

    override val id: Column<EntityID<Long>> = long("id")
        .autoIncrement()
        .entityId()

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(id)

    val shop = reference(name = "shop", foreign = ShopTable, onDelete = ReferenceOption.CASCADE)

    val name = citext("name", 32)
    val description = citext("description", 255).nullable()
    val brandName = text("brandname").nullable()
    val price = decimal("price", precision = 15, scale = 2).nullable()
    val category = reference("category_id", ShopProductCategoryTable)
    val imageUrls = jsonb(
        name = "photoUrls",
        serialize = StringListSerializer::serialize,
        deserialize = StringListSerializer::deserialize
    )
    val seasonId = integer("season_id").nullable()
    val color = optReference("color", ShopProductColorsTable)
}
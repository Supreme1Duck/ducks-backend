package com.ducks.shops.common.database.table

import com.ducks.util.StringListSerializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.json.jsonb
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
    val mainImageUrl = text("main_image_url")
    val imageUrls = jsonb(
        name = "photoUrls",
        serialize = StringListSerializer::serialize,
        deserialize = StringListSerializer::deserialize,
    )
    val seasonId = integer("season_id").nullable()
    val color = optReference("color", ShopProductColorsTable)
}
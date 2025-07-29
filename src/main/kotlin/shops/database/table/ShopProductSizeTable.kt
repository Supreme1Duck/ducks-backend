package com.ducks.shops.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object ShopProductSizeTable: LongIdTable("shop_product_size_table") {

    val name = text("name")
    val parentId = optReference("parentId", this, onDelete = ReferenceOption.CASCADE)
}
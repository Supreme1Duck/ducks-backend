package com.ducks.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object ShopProductSizeTable: LongIdTable("shop_product_size_table") {

    val name = text("name")
    val parentId = optReference("parent", this, onDelete = ReferenceOption.CASCADE)
}
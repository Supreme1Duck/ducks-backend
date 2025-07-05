package com.ducks.shops.common.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object ShopProductCategoryTable : LongIdTable(name = "shop_product_category_table") {

    val name = varchar("name", 255)
    val description = varchar("description", 255).nullable()
    val isSuperCategory = bool("isSuperCategory").default(false)
    val superCategoryId = optReference("superCategoryId", this)
    val parent = optReference("parent_id", this, onDelete = ReferenceOption.CASCADE)
}
package com.ducks.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object ShopProductCategoryTable : LongIdTable(name = "shop_product_category_table") {

    val name = varchar("name", 255)
    val description = varchar("description", 255).nullable()
    val isSuperCategory = bool("isSuperCategory").default(false)
    val superCategoryId = optReference("superCategoryId", this)
    val parent = optReference("parent_id", this, onDelete = ReferenceOption.CASCADE)
}
package com.ducks.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object ShopProductsWithSizesTable : LongIdTable(name = "shop_products_with_sizes") {

    var product = reference("product", ShopProductTable, onDelete = ReferenceOption.CASCADE)
    var size = reference("size", ShopProductSizeTable, onDelete = ReferenceOption.CASCADE)
}
package com.ducks.shops.database.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object ShopProductsWithSizesTable : LongIdTable(name = "shop_products_with_sizes") {

    var product = reference("product", ShopProductTable, onDelete = ReferenceOption.CASCADE)
    var size = reference("size", ShopProductSizeTable, onDelete = ReferenceOption.CASCADE)
}
package com.ducks.shops.common.database.table

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object ShopProductColorsTable: IntIdTable("ducks_shop_product_colors_table") {

    var name = text("name").uniqueIndex()
}
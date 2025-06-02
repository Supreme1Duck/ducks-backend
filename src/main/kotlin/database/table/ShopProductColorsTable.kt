package com.ducks.database.table

import org.jetbrains.exposed.dao.id.IntIdTable

object ShopProductColorsTable: IntIdTable("ducks_shop_product_colors_table") {

    var name = text("name").uniqueIndex()
}
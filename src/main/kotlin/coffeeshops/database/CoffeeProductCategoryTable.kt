package com.ducks.coffeeshops.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeProductCategoryTable: LongIdTable("ducks_coffee_product_category_table") {

    val name = text("name")
}
package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeConstructorCategoryTable : LongIdTable("ducks_coffee_constructor_categories_table") {

    val name = text("name")
    val defaultConstructorId = long("default_constructor_id").nullable()
    val maxSelection = integer("max_selection").nullable()
    val shopId = reference("shop_id", CoffeeShopTable, onDelete = ReferenceOption.CASCADE)
}
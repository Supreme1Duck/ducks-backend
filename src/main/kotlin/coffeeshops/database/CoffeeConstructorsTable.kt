package com.ducks.coffeeshops.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeConstructorsTable : LongIdTable("ducks_coffee_constructors_table") {

    val name = text("name")
    val price = decimal("price", precision = 15, scale = 2).nullable()

    val categoryId = reference("category_id", CoffeeConstructorCategoryTable, onDelete = ReferenceOption.CASCADE)
    val shopId = reference("shop_id", CoffeeShopTable, onDelete = ReferenceOption.CASCADE)

    val isInStock = bool("is_in_stock").default(true)
}
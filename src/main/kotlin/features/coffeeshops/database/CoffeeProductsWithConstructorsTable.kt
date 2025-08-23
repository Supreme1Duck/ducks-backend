package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeProductsWithConstructorsTable : LongIdTable("ducks_coffee_products_with_constructors_table") {

    val constructor = reference("constructor", CoffeeConstructorsTable, onDelete = ReferenceOption.CASCADE)
    val category = reference("category", CoffeeConstructorCategoryTable, onDelete = ReferenceOption.CASCADE)
    val product = reference("product", CoffeeProductTable, onDelete = ReferenceOption.CASCADE)
}
package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Категория конструкторов для всего кофешопа.
 * Эта таблица используется при связи Кофешоп-Конструкторы.
 * Для связи Продукт-Конструкторы использовать [CoffeeModifiedConstructorCategoryTable]
 */

object CoffeeConstructorCategoryTable : LongIdTable("ducks_coffee_constructor_categories_table") {

    val name = text("name")
    val shopId = reference("shop_id", CoffeeShopTable, onDelete = ReferenceOption.CASCADE)
}
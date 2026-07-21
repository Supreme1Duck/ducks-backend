package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeProductsWithConstructorsTable : LongIdTable("ducks_coffee_products_with_constructors_table") {

    val constructor = reference("constructor", CoffeeConstructorsTable, onDelete = ReferenceOption.CASCADE)
    val modifiedCategory = reference("category", CoffeeModifiedConstructorCategoryTable, onDelete = ReferenceOption.CASCADE)
    val product = reference("product", CoffeeProductTable, onDelete = ReferenceOption.CASCADE)

    /**
     * Один конструктор может быть привязан к продукту только один раз.
     *
     * Без этого ограничения продавец мог положить один и тот же конструктор в две
     * модифицированные категории продукта, и в таблице появлялись две строки связи,
     * различающиеся только [modifiedCategory]. Выборка конструкторов по
     * (product, constructor) возвращала обе строки, из-за чего при создании заказа
     * добавка попадала в состав дважды и её цена считалась дважды.
     */
    init {
        uniqueIndex(product, constructor)
    }
}
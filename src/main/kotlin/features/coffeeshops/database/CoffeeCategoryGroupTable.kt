package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Верхний уровень над [CoffeeProductCategoryTable]: Напитки / Еда / Дополнительно.
 * Ровно два уровня — у группы нет ссылки на родителя, вложить группу в группу нельзя.
 */
object CoffeeCategoryGroupTable: LongIdTable("ducks_coffee_category_group_table") {

    val name = text("name")

    /** Порядок показа групп в меню. */
    val sortOrder = integer("sort_order")
}

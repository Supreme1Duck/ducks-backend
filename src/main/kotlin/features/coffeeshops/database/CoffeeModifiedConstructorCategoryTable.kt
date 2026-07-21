package com.ducks.features.coffeeshops.database


import com.ducks.util.LongListSerializer
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.jsonb

/**
 * Категория для конкретного продукта.
 * Может иметь/не иметь модификаций типа выбора по умолчанию.
 * Эта таблица используется при связи Продукт-Конструкторы.
 * Для связи Кофешоп-Конструкторы использовать [CoffeeConstructorCategoryTable]
 */
object CoffeeModifiedConstructorCategoryTable : LongIdTable("ducks_coffee_modified_constructor_categories_table") {

    val categoryId = reference(
        name = "categoryId",
        CoffeeConstructorCategoryTable,
        onDelete = ReferenceOption.CASCADE,
    )

    val defaultConstructorIds = jsonb(
        name = "defaultConstructorIds",
        serialize = LongListSerializer::serialize,
        deserialize = LongListSerializer::deserialize,
    ).nullable()

    val maxSelection = integer("max_selection").nullable()
    val minSelection = integer("min_selection").nullable()
}
package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCoffeeCategoryRequest(
    val data: List<CategoriesForGroup>,
)

/** Пачка новых категорий для одной группы: см. ducks_coffee_category_group_table. */
@Serializable
data class CategoriesForGroup(
    val groupId: Long,
    val names: List<String>,
)

package com.ducks.features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeCategoryDTO(
    val id: Long,
    val name: String,
)

@Serializable
data class CoffeeCategoryWithCountDTO(
    val id: Long,
    val name: String,
    val productCount: Long,
)

@Serializable
data class CoffeeCategoryGroupDTO(
    val id: Long,
    val name: String,
    val sortOrder: Int,
)

/**
 * Категория вместе с её группой (Напитки / Еда / Дополнительно).
 * Отдаётся только селлеру в GET /categories — клиенту группы не нужны.
 */
@Serializable
data class CoffeeCategoryWithGroupDTO(
    val id: Long,
    val name: String,
    val group: CoffeeCategoryGroupDTO,
)

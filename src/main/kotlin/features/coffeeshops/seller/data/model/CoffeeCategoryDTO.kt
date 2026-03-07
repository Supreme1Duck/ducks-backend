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

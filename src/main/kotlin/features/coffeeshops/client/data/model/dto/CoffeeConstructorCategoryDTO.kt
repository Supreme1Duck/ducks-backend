package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeConstructorCategoryDTO(
    val id: Long,
    val name: String,
    val defaultConstructorId: Long?,
    val maxSelection: Int?,
)
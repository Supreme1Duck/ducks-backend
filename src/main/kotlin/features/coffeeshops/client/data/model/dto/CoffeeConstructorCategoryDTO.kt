package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeConstructorCategoryDTO(
    val id: Long,
    val name: String,
    val defaultConstructorIds: List<Long>?,
    val maxSelection: Int?,
    val minSelection: Int?,
)
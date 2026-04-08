package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CookingTimeEstimateDTO(
    val minutesToCook: Int,
)

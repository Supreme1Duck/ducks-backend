package com.ducks.features.coffeeshops.seller.routings.request.shop

import kotlinx.serialization.Serializable

@Serializable
data class SetCookingModeRequest(
    val mode: Int,
)

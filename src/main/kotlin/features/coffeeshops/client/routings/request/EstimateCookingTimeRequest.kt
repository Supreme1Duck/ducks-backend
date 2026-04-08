package com.ducks.features.coffeeshops.client.routings.request

import kotlinx.serialization.Serializable

@Serializable
data class EstimateCookingTimeRequest(
    val productIds: List<Long>,
)

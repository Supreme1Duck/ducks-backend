package com.ducks.features.coffeeshops.client.routings.request

import kotlinx.serialization.Serializable

@Serializable
data class OrderTimeRequest(
    val shopId: Long,
    val productIds: List<Long>,
)

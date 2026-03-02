package com.ducks.features.coffeeshops.seller.routings.request.products

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCoffeeProductRequest(
    val productId: Long,
)

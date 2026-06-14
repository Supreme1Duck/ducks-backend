package com.ducks.features.coffeeshops.seller.routings.request.products

import kotlinx.serialization.Serializable

@Serializable
data class DeleteCoffeeProductRequest(
    val productId: Long,
)

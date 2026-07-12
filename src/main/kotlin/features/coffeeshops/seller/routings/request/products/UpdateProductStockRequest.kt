package com.ducks.features.coffeeshops.seller.routings.request.products

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductStockRequest(
    val productId: Long,
    val inStock: Boolean,
)

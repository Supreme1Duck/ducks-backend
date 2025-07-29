package com.ducks.shops.seller.routing.request

import kotlinx.serialization.Serializable

@Serializable
data class DeleteProductRequest(
    val productId: Long,
)

package com.ducks.features.shops.seller.routing.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    val id: Long,
)
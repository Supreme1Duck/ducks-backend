package com.ducks.features.coffeeshops.seller.routings.request.constructor

import kotlinx.serialization.Serializable

@Serializable
data class SetInStockRequest(
    val constructorId: Long,
    val isInStock: Boolean,
)
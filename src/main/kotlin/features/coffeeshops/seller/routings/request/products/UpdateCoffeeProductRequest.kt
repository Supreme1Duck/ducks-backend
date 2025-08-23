package com.ducks.features.coffeeshops.seller.routings.request.products

import com.ducks.common.data.UpdateMap
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCoffeeProductRequest(
    val productId: Long,
    val updateMap: UpdateMap,
)

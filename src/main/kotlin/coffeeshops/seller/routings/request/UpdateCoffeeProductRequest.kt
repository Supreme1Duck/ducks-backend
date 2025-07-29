package com.ducks.coffeeshops.seller.routings.request

import com.ducks.common.data.UpdateMap
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCoffeeProductRequest(
    val productId: Long,
    val updateMap: UpdateMap,
)

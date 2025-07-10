package com.ducks.shops.seller.routing.request

import com.ducks.shops.seller.data.model.UpdateMap
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    val id: Long,
    val updateMap: UpdateMap,
)
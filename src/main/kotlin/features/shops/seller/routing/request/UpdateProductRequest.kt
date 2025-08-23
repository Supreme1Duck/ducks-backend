package com.ducks.features.shops.seller.routing.request

import com.ducks.common.data.UpdateMap
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProductRequest(
    val id: Long,
    val updateMap: UpdateMap,
)
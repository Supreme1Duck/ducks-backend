package com.ducks.features.coffeeshops.seller.routings.request.shop

import com.ducks.common.data.UpdateMap
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCoffeeShopRequest(
    val shopId: Long,
    val updateMap: UpdateMap,
)

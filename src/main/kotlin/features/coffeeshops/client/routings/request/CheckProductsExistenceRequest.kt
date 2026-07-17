package com.ducks.features.coffeeshops.client.routings.request

import com.ducks.features.coffeeshops.client.data.model.dto.ShopProductPair
import kotlinx.serialization.Serializable

@Serializable
data class CheckProductsExistenceRequest(
    val pairs: List<ShopProductPair>,
)

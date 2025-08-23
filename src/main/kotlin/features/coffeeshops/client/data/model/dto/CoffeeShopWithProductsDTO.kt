package com.ducks.features.coffeeshops.client.data.model.dto

import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopWithProductsDTO(
    val shop: CoffeeShopDetailsDTO,
    val products: Map<String, CoffeeShopProductPreviewDTO>,
)
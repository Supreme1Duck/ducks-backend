package com.ducks.coffeeshops.common.data.model.dto

import com.ducks.coffeeshops.common.data.model.preview.CoffeeShopProductPreviewDTO
import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopWithProductsDTO(
    val shop: CoffeeShopDetailsDTO,
    val products: Map<String, CoffeeShopProductPreviewDTO>,
)
package com.ducks.features.coffeeshops.client.data.model.dto

import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopWithProductsDTO(
    val shop: CoffeeShopDetailsDTO,
    val products: List<ProductsByCategoryDTO>,
)

@Serializable
data class ProductsByCategoryDTO(
    val category: CoffeeCategoryDTO,
    val products: List<CoffeeShopProductPreviewDTO>,
)
package com.ducks.features.coffeeshops.client.data.model.preview

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.client.data.model.dto.NutrientsDTO
import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopProductPreviewDTO(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val categoryId: Long,
    val categoryName: String,
    val inStock: Boolean,
    val minutesToCook: Int?,
    val shopId: Long,
    val shopName: String,
    val sizes: List<CoffeeProductSizeDTO>,
    val constructors: List<CoffeeConstructorsDTO>,
    val description: String?,
    val nutrients: NutrientsDTO?,
)

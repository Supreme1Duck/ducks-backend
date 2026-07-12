package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CoffeeProductWithDetailsDTO(
    val id: Long,
    val shopId: Long,
    val name: String,
    val imageUrl: String,
    @Serializable(with = BigDecimalSerializer::class)
    val minPrice: BigDecimal,
    val minSize: String,
    val inStock: Boolean,
    val categoryId: Long,
    val minutesToCook: Int,
    val sizes: List<CoffeeProductSizeDTO>,
    val constructors: List<CoffeeConstructorsDTO>,
    val description: String?,
    val nutrients: NutrientsDTO?,
)

@Serializable
data class CoffeeConstructorsDTO(
    val category: CoffeeConstructorCategoryDTO,
    val constructors: List<CoffeeConstructorDTO>,
)

@Serializable
data class NutrientsDTO(
    val calories: Int?,
    val carbohydrates: Int?,
    val protein: Int?,
    val fats: Int?,
)
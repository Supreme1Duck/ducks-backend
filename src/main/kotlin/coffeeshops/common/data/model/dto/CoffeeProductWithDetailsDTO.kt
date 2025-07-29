package com.ducks.coffeeshops.common.data.model.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CoffeeProductWithDetailsDTO(
    val id: Long,
    val name: String,
    val imageUrl: String,
    @Serializable(with = BigDecimalSerializer::class)
    val minPrice: BigDecimal,
    val minSize: String,
    val description: String?,
    val nutrients: NutrientsDTO?,
)

@Serializable
data class NutrientsDTO(
    val calories: String?,
    val carbohydrates: String?,
    val protein: String?,
    val fats: String?,
)
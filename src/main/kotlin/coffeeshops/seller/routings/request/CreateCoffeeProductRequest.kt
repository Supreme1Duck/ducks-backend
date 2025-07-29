package com.ducks.coffeeshops.seller.routings.request

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CreateCoffeeProductRequest(
    val name: String,
    val description: String? = null,

    val sizes: List<CoffeeProductSizeRequest>,
    val categoryId: Long,

    val imageUrl: String,

    val carbohydrates: String? = null,
    val protein: String? = null,
    val fats: String? = null,
    val calories: String? = null,
)

@Serializable
data class CoffeeProductSizeRequest(
    val sizeName: String? = null,
    val sizeValue: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
)

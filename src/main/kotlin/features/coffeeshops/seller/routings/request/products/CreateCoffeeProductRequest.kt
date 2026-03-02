package com.ducks.features.coffeeshops.seller.routings.request.products

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

    val minutesToCook: Int?,

    val constructors: List<CoffeeCreateConstructorRequest>? = null,

    val carbohydrates: String? = null,
    val protein: String? = null,
    val fats: String? = null,
    val calories: String? = null,
)

@Serializable
data class CoffeeProductSizeRequest(
    val id: String,
    val sizeName: String? = null,
    val sizeValue: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)

@Serializable
data class CoffeeCreateConstructorRequest(
    val category: CoffeeCategoryConstructorRequest,
    val constructors: List<CoffeeConstructorRequest>,
)

@Serializable
data class CoffeeCategoryConstructorRequest(
    val id: Long,
    val defaultConstructorIds: List<Long>? = null,
    val maxSelection: Int? = null,
    val minSelection: Int? = null,
)

@Serializable
data class CoffeeConstructorRequest(
    val id: Long,
)

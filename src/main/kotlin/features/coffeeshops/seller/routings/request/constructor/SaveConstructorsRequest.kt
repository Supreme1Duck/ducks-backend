package com.ducks.features.coffeeshops.seller.routings.request.constructor

import kotlinx.serialization.Serializable

@Serializable
data class SaveConstructorsRequest(
    val categories: List<CategoryWithConstructorsRequest>
)

@Serializable
data class CategoryWithConstructorsRequest(
    val id: Long,
    val categoryName: String,
    val constructors: List<ConstructorItemRequest>
)

@Serializable
data class ConstructorItemRequest(
    val id: Long,
    val name: String,
    val price: Float?,
    val isInStock: Boolean,
)

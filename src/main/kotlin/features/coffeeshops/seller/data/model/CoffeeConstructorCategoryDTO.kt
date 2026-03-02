package com.ducks.features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SellerCoffeeConstructorCategoryDTO(
    val id: Long,
    val name: String,
)
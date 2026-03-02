package com.ducks.features.coffeeshops.seller.data.model

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorDTO
import kotlinx.serialization.Serializable

@Serializable
data class SellerCoffeeCategoriesWithConstructorsDTO(
    val data: List<SellerCoffeeCategoryDTO>,
)

@Serializable
data class SellerCoffeeCategoryDTO(
    val category: SellerCoffeeConstructorCategoryDTO,
    val constructors: List<CoffeeConstructorDTO>,
)


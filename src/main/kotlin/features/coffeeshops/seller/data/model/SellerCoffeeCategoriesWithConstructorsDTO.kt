package com.ducks.features.coffeeshops.seller.data.model

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorCategoryDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorDTO
import kotlinx.serialization.Serializable

@Serializable
data class SellerCoffeeCategoriesWithConstructorsDTO(
    val data: Map<CoffeeConstructorCategoryDTO, CoffeeConstructorDTO>,
)


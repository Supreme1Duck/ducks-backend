package com.ducks.coffeeshops.seller.data.model

import com.ducks.coffeeshops.common.data.model.dto.CoffeeConstructorCategoryDTO
import com.ducks.coffeeshops.common.data.model.dto.CoffeeConstructorDTO
import kotlinx.serialization.Serializable

@Serializable
data class SellerCoffeeCategoriesWithConstructorsDTO(
    val data: Map<CoffeeConstructorCategoryDTO, CoffeeConstructorDTO>,
)


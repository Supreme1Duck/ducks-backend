package com.ducks.features.coffeeshops.seller.routings.request.constructor

import kotlinx.serialization.Serializable

@Serializable
data class CreateConstructorCategoryRequest(
    val name: String,
)
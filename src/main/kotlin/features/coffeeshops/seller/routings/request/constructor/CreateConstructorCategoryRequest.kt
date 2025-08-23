package com.ducks.coffeeshops.seller.routings.request.constructor

import kotlinx.serialization.Serializable

@Serializable
data class CreateConstructorCategoryRequest(
    val name: String,
    val defaultConstructorId: Long?,
    val maxSelection: Int?,
)
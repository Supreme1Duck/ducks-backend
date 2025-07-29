package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCoffeeCategoryRequest(
    val data: List<String>,
)
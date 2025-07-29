package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCoffeeShopRequest(
    val name: String,
    val address: String,
    val unp: String,
    val initialPass: String,
)
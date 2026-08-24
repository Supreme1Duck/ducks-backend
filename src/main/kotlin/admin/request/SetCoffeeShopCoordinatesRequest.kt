package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class SetCoffeeShopCoordinatesRequest(
    val shopId: Long,
    val latitude: Double,
    val longitude: Double,
)

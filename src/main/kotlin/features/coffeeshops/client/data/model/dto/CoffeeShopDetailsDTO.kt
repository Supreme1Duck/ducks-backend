package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopDetailsDTO(
    val id: Long,
    val name: String,
    val address: String,
    val lowestPrice: Int?,
    val workTime: String,
    val tags: List<String>?,
    val seatsCapacity: Int,
    val closestTime: Int,
)
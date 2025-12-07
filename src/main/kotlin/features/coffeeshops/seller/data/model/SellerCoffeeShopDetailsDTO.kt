package com.ducks.features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SellerCoffeeShopDetailsDTO(
    val id: Long,
    val name: String,
    val address: String,
    val lowestPrice: Int?,
    val workTime: String,
    val tags: List<String>?,
    val seatsCapacity: Int,
    val closestTime: Long?,
)
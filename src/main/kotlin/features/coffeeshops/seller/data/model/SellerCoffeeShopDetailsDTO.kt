package com.ducks.features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SellerCoffeeShopDetailsDTO(
    val id: Long,
    val name: String,
    val address: String,
    val description: String?,
    val imageUrls: List<String>?,
    val lowestPrice: Int?,
    val workTime: String,
    val isTemporaryClosed: Boolean,
    val schedule: List<Schedule>,
    val tags: List<String>?,
    val tablesCapacity: Int,
    val freeTables: Int,
    val cookingMode: Int,
    val closestTimeToTakeOrder: Long?,
    val canTakeOrdersReason: Int?,
    val activePause: SellerCoffeeShopActivePauseDTO?,
) {

    @Serializable
    data class Schedule(
        val dayOfWeek: String,
        val startTime: String,
        val endTime: String,
        val isClosed: Boolean,
    )
}
package com.ducks.features.coffeeshops.seller.routings.request.shop

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCoffeeShopRequest(
    val address: String,
    val description: String,
    val photoUrls: List<String>,
    val tags: List<String>,
    val schedule: List<ScheduleDay>,
) {

    @Serializable
    data class ScheduleDay(
        val name: String,
        val openTime: String?,
        val closeTime: String?,
        val isClosed: Boolean,
    )
}

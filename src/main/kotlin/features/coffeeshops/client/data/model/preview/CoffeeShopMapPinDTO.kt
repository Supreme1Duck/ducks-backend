package com.ducks.features.coffeeshops.client.data.model.preview

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopMapPinDTO(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isTemporaryClosed: Boolean,
    val rating: Double,
    val pricesStartsFrom: Int? = null,
    val image: String? = null,
    val distanceKm: Double? = null,
    val openTime: Long? = null,
    val closeTime: Long? = null,
    val isClosed: Boolean = false,
)

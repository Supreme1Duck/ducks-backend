package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class WorkTimeDTO(
    val openTime: Long,
    val closeTime: Long,
)

@Serializable
data class CoffeeShopDetailsDTO(
    val id: Long,
    val name: String,
    val address: String,
    val imageUrls: List<String>?,
    val lowestPrice: Int?,
    val workTime: WorkTimeDTO?,
    val tags: List<String>?,
    val isTemporaryClosed: Boolean,
    val tablesCapacity: Int,
    val freeTables: Int,
    val closestTime: Long?,
    val closestTimeReason: Int,
    val rating: Double,
)
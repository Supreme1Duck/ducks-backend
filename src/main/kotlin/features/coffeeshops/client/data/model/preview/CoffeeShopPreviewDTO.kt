package com.ducks.features.coffeeshops.client.data.model.preview

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopPreviewDTO(
    val id: Long,
    val name: String,
    val address: String,
    val images: List<String>,
    val tags: List<String>?,
    val isTemporaryClosed: Boolean,
    val pricesStartsFrom: Int? = null,
)

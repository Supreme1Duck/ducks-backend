package com.ducks.coffeeshops.common.data.model.preview

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopPreviewDTO(
    val id: Long,
    val name: String,
    val address: String,
    val tags: List<String>?,
    val pricesStartsFrom: Int? = null,
)

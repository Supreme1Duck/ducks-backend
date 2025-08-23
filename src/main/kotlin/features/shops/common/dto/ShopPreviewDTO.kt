package com.ducks.features.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopPreviewDTO(
    val id: Long,
    val name: String,
    val address: String,
    val products: List<ShopProductPreviewDTO>,
)
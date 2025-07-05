package com.ducks.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductPreviewDTO(
    val id: Long,
    val imgUrl: String,
)
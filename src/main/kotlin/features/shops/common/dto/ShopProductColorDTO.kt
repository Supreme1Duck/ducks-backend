package com.ducks.features.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductColorDTO(
    val id: Int,
    val name: String,
)

package com.ducks.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductColorDTO(
    val id: Int,
    val name: String,
)

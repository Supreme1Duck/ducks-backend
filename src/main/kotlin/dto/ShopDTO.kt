package com.ducks.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopDTO(
    val id: Long,
    val name: String,
    val description: String?,
    val address: String,
    val photoUrls: List<String>,
    val products: List<ShopProductDTO>,
)

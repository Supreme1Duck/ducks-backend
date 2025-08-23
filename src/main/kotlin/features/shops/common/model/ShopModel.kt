package com.ducks.features.shops.common.model

data class ShopModel(
    val name: String,
    val address: String,
    val description: String? = null,
    val photoUrls: List<String> = emptyList(),
)

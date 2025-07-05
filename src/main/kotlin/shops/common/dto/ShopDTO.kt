package com.ducks.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopDTO(
    val id: Long,
    val name: String,
    val description: String?,
    val address: String,
    val photoUrls: List<String>,
    val products: List<SearchShopProductDTO>,
    val filters: ShopProductFiltersDTO,
)

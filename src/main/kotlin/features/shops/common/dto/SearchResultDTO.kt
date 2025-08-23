package com.ducks.features.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDTO(
    val products: List<SearchShopProductDTO>,
    val filters: ShopProductFiltersDTO,
)

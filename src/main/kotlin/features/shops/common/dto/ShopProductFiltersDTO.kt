package com.ducks.features.shops.common.dto

import com.ducks.features.shops.common.model.SeasonModel
import kotlinx.serialization.Serializable

@Serializable
data class ShopProductFiltersDTO(
    val sizes: List<ShopProductChildSizeDTO>,
    val categories: List<ShopProductCategoryDTO>,
    val colors: List<ShopProductColorDTO>,
    val seasons: List<SeasonModel>,
)
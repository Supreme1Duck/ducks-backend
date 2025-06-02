package com.ducks.dto

import com.ducks.model.SeasonModel
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ShopProductDTO(
    val id: Long,
    val name: String,
    val description: String?,
    @Contextual
    val price: BigDecimal?,
    val categoryDTO: ShopProductCategoryDTO,
    val color: ColorDTO?,
    val season: SeasonModel?,
    val shopId: Long,
    val shopName: String,
    val brandName: String?,
    val imageUrls: List<String>,
)

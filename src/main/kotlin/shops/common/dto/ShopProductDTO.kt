package com.ducks.shops.common.dto

import com.ducks.shops.common.model.SeasonModel
import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ShopProductDTO(
    val id: Long,
    val name: String,
    val description: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
    val categoryDTO: ShopProductCategoryDTO,
    val color: ShopProductColorDTO?,
    val season: SeasonModel?,
    val shopId: Long,
    val shopName: String,
    val brandName: String?,
    val imageUrls: List<String>,
    val sizes: List<ShopProductChildSizeDTO>,
)

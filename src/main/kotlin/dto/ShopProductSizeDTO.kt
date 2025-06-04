package com.ducks.dto

import kotlinx.serialization.Serializable

/**
 * Это DTO используется только для получения списка всех доступных размеров
 */
@Serializable
data class ShopProductSizeDTO(
    val id: Long,
    val name: String,
    val children: List<ShopProductChildSizeDTO>
)

@Serializable
data class ShopProductChildSizeDTO(
    val id: Long,
    val parentId: Long,
    val name: String,
)
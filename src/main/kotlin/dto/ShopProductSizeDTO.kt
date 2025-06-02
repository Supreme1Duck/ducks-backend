package com.ducks.dto

/**
 * Это DTO используется только для получения списка всех доступных размеров
 */
data class ShopProductSizeDTO(
    val id: Long,
    val name: String,
    val children: List<ShopProductChildSizeDTO>
)

data class ShopProductChildSizeDTO(
    val id: Long,
    val parentId: Long,
    val name: String,
)
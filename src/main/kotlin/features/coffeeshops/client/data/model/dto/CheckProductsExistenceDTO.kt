package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductPair(
    val shopId: Long,
    val productId: Long,
)

@Serializable
data class CheckProductsExistenceResponse(
    // Пары, которых нет в базе (продукт не принадлежит указанной кофейне или не существует).
    val missing: List<ShopProductPair>,
)

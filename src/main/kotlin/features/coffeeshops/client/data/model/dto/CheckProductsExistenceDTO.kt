package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ShopProductPair(
    val shopId: Long,
    val productId: Long,
)

/** Актуальные данные товара, которые клиент держит у себя в избранном. */
@Serializable
data class ProductSnapshotDTO(
    val shopId: Long,
    val productId: Long,
    val name: String,
    val imageUrl: String,
    val shopName: String,
    // Цена первого размера — та же, что клиент сохраняет при добавлении в избранное.
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
)

@Serializable
data class CheckProductsExistenceResponse(
    // Пары, которых нет в базе (продукт не принадлежит указанной кофейне или не существует).
    val missing: List<ShopProductPair>,
    // Существующие пары с актуальными данными: продавец мог сменить фото, название или цену.
    val products: List<ProductSnapshotDTO> = emptyList(),
)

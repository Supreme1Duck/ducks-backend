package com.ducks.features.coffeeshops.client.data.model.dto

import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import kotlinx.serialization.Serializable

@Serializable
data class CartRecommendationsResponse(
    val title: String,
    val products: List<CartRecommendationDTO>,
)

@Serializable
data class CartRecommendationDTO(
    val product: CoffeeShopProductPreviewDTO,
    val extraMinutes: Int,
    // true — товар добавляется в один тап: один размер и нет обязательных конструкторов.
    // false — клиент обязан открыть карточку товара, иначе добавит не то, что человек хотел.
    val isQuickAdd: Boolean,
)

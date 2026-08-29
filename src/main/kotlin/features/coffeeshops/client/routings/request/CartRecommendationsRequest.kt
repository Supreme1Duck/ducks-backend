package com.ducks.features.coffeeshops.client.routings.request

import kotlinx.serialization.Serializable

@Serializable
data class CartRecommendationsRequest(
    val shopId: Long,
    // Содержимое корзины: id повторяется столько раз, сколько штук товара в корзине.
    val productIds: List<Long>,
    val limit: Int? = null,
)

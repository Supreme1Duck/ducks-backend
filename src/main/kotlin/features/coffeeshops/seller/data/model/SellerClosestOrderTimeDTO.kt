package com.ducks.features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SellerClosestOrderTimeDTO(
    // Ближайшее время принятия заказа в миллисекундах. null, если кофешоп не может принять заказ.
    val closestTimeToTakeOrder: Long?,
    // IsCoffeeShopReadyToOrderReason: 0 - готов, 1 - только короткий заказ, 2 - закрыт, 3 - много заказов.
    val canTakeOrdersReason: Int,
)

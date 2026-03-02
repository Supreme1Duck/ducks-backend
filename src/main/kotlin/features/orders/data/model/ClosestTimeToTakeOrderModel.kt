package com.ducks.features.orders.data.model

data class ClosestTimeToTakeOrderModel(
    val closestTime: Long?,
    val reason: IsCoffeeShopReadyToOrderReason,
)

enum class IsCoffeeShopReadyToOrderReason(val value: Int) {
    ReadyToTake(0),
    OnlyShortOrder(1),
    ShopClosed(2),
    TooManyOrders(3),
}
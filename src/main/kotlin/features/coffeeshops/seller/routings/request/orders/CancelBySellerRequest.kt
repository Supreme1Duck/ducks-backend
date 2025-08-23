package com.ducks.features.coffeeshops.seller.routings.request.orders

import kotlinx.serialization.Serializable

@Serializable
class CancelBySellerRequest(
    val orderId: Long,
    val message: String?,
)
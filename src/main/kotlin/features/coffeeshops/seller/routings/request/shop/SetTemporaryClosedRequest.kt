package com.ducks.features.coffeeshops.seller.routings.request.shop

import kotlinx.serialization.Serializable

@Serializable
data class SetTemporaryClosedRequest(
    val isClosed: Boolean,
    val reason: String? = null,
)

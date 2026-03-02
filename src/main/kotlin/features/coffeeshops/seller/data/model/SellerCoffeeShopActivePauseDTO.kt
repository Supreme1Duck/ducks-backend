package com.ducks.features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable

@Serializable
class SellerCoffeeShopActivePauseDTO(
    val startsAt: Long?,
    val endsAt: Long?,
)
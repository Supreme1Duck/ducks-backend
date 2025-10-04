package com.ducks.features.coffeeshops.seller.routings.request.shop

import kotlinx.serialization.Serializable

@Serializable
data class SetTechnicalPauseRequest(
    val startsAt: Long,
    val endsAt: Long,
)
package com.ducks.features.landing.route.request

import kotlinx.serialization.Serializable

@Serializable
data class CallbackRequest(
    val phoneNumber: String,
)

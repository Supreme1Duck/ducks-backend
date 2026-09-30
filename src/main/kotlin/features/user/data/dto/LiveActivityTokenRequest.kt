package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveActivityTokenRequest(val token: String)

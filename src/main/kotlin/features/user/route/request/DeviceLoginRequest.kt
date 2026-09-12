package com.ducks.features.user.route.request

import kotlinx.serialization.Serializable

@Serializable
data class DeviceLoginRequest(
    val firstName: String? = null,
)

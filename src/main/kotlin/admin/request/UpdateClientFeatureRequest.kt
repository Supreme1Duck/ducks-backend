package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateClientFeatureRequest(
    val isEnabled: Boolean,
)

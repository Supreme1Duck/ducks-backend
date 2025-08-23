package com.ducks.features.coffeeshops.seller.routings.request.constructor

import kotlinx.serialization.Serializable

@Serializable
data class DeleteConstructorRequest(
    val containerId: Long,
    val categoryId: Long,
)
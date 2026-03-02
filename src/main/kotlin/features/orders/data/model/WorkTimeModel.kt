package com.ducks.features.orders.data.model

data class WorkTimeModel(
    val dayOfWeek: Int,
    val startTime: Long,
    val endTime: Long,
    val isClosed: Boolean,
)
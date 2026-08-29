package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CookingTimeEstimateDTO(
    val minutesToCook: Int,
    // Ближайшее время, к которому заказ такой длительности реально будет готов,
    // в миллисекундах. То же значение, что первым отдаёт /shop/order-time.
    // null, если свободного времени у кофешопа нет.
    val estimatedFinishTime: Long? = null,
    // Сколько минут осталось до готовности: estimatedFinishTime - сейчас. null, если estimatedFinishTime отсутствует.
    val minutesToFinish: Int? = null,
)

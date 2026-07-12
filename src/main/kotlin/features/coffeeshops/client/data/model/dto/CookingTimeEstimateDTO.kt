package com.ducks.features.coffeeshops.client.data.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CookingTimeEstimateDTO(
    val minutesToCook: Int,
    // closestTimeToTakeOrder + minutesToCook, в миллисекундах. null, если у кофешопа нет ближайшего времени приёма заказа.
    val estimatedFinishTime: Long? = null,
    // Сколько минут осталось до готовности: estimatedFinishTime - сейчас. null, если estimatedFinishTime отсутствует.
    val minutesToFinish: Int? = null,
)

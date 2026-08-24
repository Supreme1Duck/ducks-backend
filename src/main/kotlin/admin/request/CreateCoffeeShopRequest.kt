package com.ducks.admin.request

import com.ducks.features.coffeeshops.seller.routings.request.shop.SetCoffeeShopScheduleRequest
import kotlinx.serialization.Serializable

@Serializable
data class CreateCoffeeShopRequest(
    val name: String,
    val address: String,
    val unp: String,
    val initialPass: String,
    val rating: Double,
    val pinCode: String,
    // Координаты кофейни (WGS84). Без них кофейня попадёт в конец списка «ближайшие».
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Список из 7 элементов, с пн по пт.
    val workTime: SetCoffeeShopScheduleRequest,
)
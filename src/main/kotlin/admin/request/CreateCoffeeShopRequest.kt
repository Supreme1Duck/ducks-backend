package com.ducks.admin.request

import com.ducks.features.coffeeshops.seller.routings.request.shop.WorkTime
import kotlinx.serialization.Serializable

@Serializable
data class CreateCoffeeShopRequest(
    val name: String,
    val address: String,
    val unp: String,
    val initialPass: String,
    // Список из 7 элементов, с пн по пт.
    val workTime: List<WorkTime>
)
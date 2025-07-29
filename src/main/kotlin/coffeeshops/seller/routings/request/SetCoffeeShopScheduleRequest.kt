package com.ducks.coffeeshops.seller.routings.request

import kotlinx.serialization.Serializable

@Serializable
data class SetCoffeeShopScheduleRequest(
    val schedule: Map<String, WorkTime?>,
)

@Serializable
data class WorkTime(
    val startTime: String,
    val endTime: String,
)

const val MONDAY_KEY = "monday"
const val TUESDAY_KEY = "tuesday"
const val WEDNESDAY_KEY = "wednesday"
const val THURSDAY_KEY = "thursday"
const val FRIDAY_KEY = "friday"
const val SATURDAY_KEY = "saturday"
const val SUNDAY_KEY = "sunday"
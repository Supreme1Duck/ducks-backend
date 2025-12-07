package com.ducks.features.coffeeshops.seller.routings.request.shop

import kotlinx.serialization.Serializable

@Serializable
data class SetCoffeeShopScheduleRequest(
    val schedule: Map<DayOfWeek, WorkTime?>,
)

@Serializable
data class WorkTime(
    val startTime: String,
    val endTime: String,
)

typealias DayOfWeek = String

const val MONDAY_KEY = "monday"
const val TUESDAY_KEY = "tuesday"
const val WEDNESDAY_KEY = "wednesday"
const val THURSDAY_KEY = "thursday"
const val FRIDAY_KEY = "friday"
const val SATURDAY_KEY = "saturday"
const val SUNDAY_KEY = "sunday"
package com.ducks.features.orders.data.repository

import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import features.orders.data.model.BusyTimeSlotsData
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class FetchAvailableOrdersTimeListRepository {

    suspend fun invoke(
        shopId: Long,
        estimatedOrderFinishTimeInMinutes: Int,
    ): List<Long>? {
        return newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()
            val maxOrderTime = currentTime + 60 * (60_000)

            val busyTimeSlots = getAllBusyTimeSlots(shopId)

            val closestTimeToTakeOrder =
                calculateClosestTimeToTakeOrder(busyTimeSlots) ?: return@newSuspendedTransaction null

            val availableOrderTimeList =
                calculateAvailableOrderTimeList(
                    orderTimeStartsFrom = closestTimeToTakeOrder,
                    maxOrderTime = maxOrderTime,
                    busyTimeSlots = busyTimeSlots,
                    estimatedOrderFinishTimeInMinutes = estimatedOrderFinishTimeInMinutes
                )

            availableOrderTimeList
        }
    }

    fun getAllBusyTimeSlots(shopId: Long): List<BusyTimeSlotsData> {
        val currentTime = System.currentTimeMillis()

        val ordersTime = CoffeeOrdersTable
            .select(
                CoffeeOrdersTable.estimatedFinishTime,
                CoffeeOrdersTable.timeToCookInMinutes,
            )
            .where {
                (CoffeeOrdersTable.coffeeShop eq shopId) and
                        (CoffeeOrdersTable.estimatedFinishTime greater currentTime) and
                        (CoffeeOrdersTable.finishedTime eq null)
            }
            .map {
                val estimatedFinishTime = it[CoffeeOrdersTable.estimatedFinishTime]!!
                val startTimeOfOrder = estimatedFinishTime - (it[CoffeeOrdersTable.timeToCookInMinutes] * 60_000)

                BusyTimeSlotsData(
                    startTime = startTimeOfOrder,
                    endTime = estimatedFinishTime,
                )
            }

        val activePauseTime = CoffeeShopTechnicalPausesTable
            .selectAll()
            .where {
                (CoffeeShopTechnicalPausesTable.coffeeShop eq shopId) and
                        (CoffeeShopTechnicalPausesTable.isActive eq true)
            }.map {
                BusyTimeSlotsData(
                    startTime = it[CoffeeShopTechnicalPausesTable.startsAt],
                    endTime = it[CoffeeShopTechnicalPausesTable.endsAt],
                )
            }

        val resultList = (ordersTime + activePauseTime).sortedBy { it.endTime }

        return resultList
    }

    fun calculateClosestTimeToTakeOrder(busyTimeSlotsDataList: List<BusyTimeSlotsData>): Long? {
        val currentTime = System.currentTimeMillis()

        if (busyTimeSlotsDataList.isEmpty()) {
            return currentTime
        }

        val tenMinsInMs = 10 * 60_000

        val startTimeOfFirstTimeSlot = busyTimeSlotsDataList.first().startTime

        val closestTimeToTakeOrders = if (currentTime + tenMinsInMs < startTimeOfFirstTimeSlot) {
            currentTime
        } else {
            var estimatedTime = 0L

            busyTimeSlotsDataList.forEachIndexed { index, value ->
                if (index == busyTimeSlotsDataList.lastIndex) {
                    estimatedTime = value.endTime
                    return@forEachIndexed
                }

                val nextIndexedValue = busyTimeSlotsDataList[index + 1]
                if (value.endTime + tenMinsInMs < nextIndexedValue.endTime - nextIndexedValue.startTime * 60_000) {
                    estimatedTime = value.endTime
                    return@forEachIndexed
                }
            }

            estimatedTime
        }

        // не больше часа от текущего времени
        return if (closestTimeToTakeOrders < currentTime + (60 * 60_000)) {
            closestTimeToTakeOrders
        } else {
            null
        }
    }

    private fun calculateAvailableOrderTimeList(
        orderTimeStartsFrom: Long,
        maxOrderTime: Long,
        busyTimeSlots: List<BusyTimeSlotsData>,
        estimatedOrderFinishTimeInMinutes: Int,
    ): List<Long> {
        val durationMs = estimatedOrderFinishTimeInMinutes * 60_000L
        val available = mutableListOf<Long>()

        var tempTime = orderTimeStartsFrom

        while (tempTime + durationMs <= maxOrderTime) {
            val orderStart = tempTime
            val orderEnd = tempTime + durationMs

            val isAvailable = busyTimeSlots.none { busySlot ->
                orderStart < busySlot.endTime && busySlot.startTime < orderEnd
            }

            if (isAvailable) {
                available.add(tempTime)
            }

            tempTime += 60_000L
        }

        return available
    }
}
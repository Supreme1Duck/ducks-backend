package com.ducks.features.orders.data.repository

import com.ducks.features.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import com.ducks.features.orders.data.model.ClosestTimeToTakeOrderModel
import com.ducks.features.orders.data.model.IsCoffeeShopReadyToOrderReason
import com.ducks.features.orders.data.model.WorkTimeModel
import com.ducks.features.orders.database.CoffeeOrdersTable
import features.orders.data.model.BusyTimeSlotsData
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

class FetchAvailableOrdersTimeListRepository {

    suspend fun invoke(
        shopId: Long,
        estimatedOrderFinishTimeInMinutes: Int,
    ): List<Long>? {
        return newSuspendedTransaction {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val maxOrderTime = currentTime + 60 * (60_000)

            val busyTimeSlots = getAllBusyTimeSlots(shopId)

            val workTime = findShopsCurrentWorkTime(shopId)

            val closestTimeToTakeOrder =
                calculateClosestTimeToTakeOrder(busyTimeSlots, coffeeShopWorkTime = workTime).closestTime
                    ?: return@newSuspendedTransaction null

            val availableOrderTimeList =
                calculateAvailableOrderTimeList(
                    orderTimeStartsFrom = closestTimeToTakeOrder,
                    maxOrderTime = maxOrderTime,
                    busyTimeSlots = busyTimeSlots,
                    estimatedOrderFinishTimeInMinutes = estimatedOrderFinishTimeInMinutes
                )

            filterByMinInterval(availableOrderTimeList, estimatedOrderFinishTimeInMinutes)
        }
    }

    fun getAllBusyTimeSlots(shopId: Long): List<BusyTimeSlotsData> {
        val currentTime = Clock.System.now().toEpochMilliseconds()

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

    fun calculateClosestTimeToTakeOrder(
        busyTimeSlotsDataList: List<BusyTimeSlotsData>,
        coffeeShopWorkTime: WorkTimeModel?,
    ): ClosestTimeToTakeOrderModel {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        val tenMinsInMs = 10 * 60_000
        val fifteenMinsInMs = 15 * 60_000

        val startTimeOfFirstTimeSlot = busyTimeSlotsDataList.firstOrNull()?.startTime

        // Если между заказами меньше 15 минут - выставляем флаг hasEnoughTimeBetweenOrders
        val (closestTimeToTakeOrders, hasEnoughTimeBetweenOrders) = if (busyTimeSlotsDataList.isEmpty()) {
            currentTime to true
        } else if (currentTime + tenMinsInMs < startTimeOfFirstTimeSlot!!) {
            // между заказами больше 15 минут
            val hasEnoughTime = currentTime + fifteenMinsInMs < startTimeOfFirstTimeSlot

            currentTime to hasEnoughTime
        } else {
            var estimatedTime = 0L
            var hasEnoughTime = true

            busyTimeSlotsDataList.forEachIndexed { index, slot ->
                if (index == busyTimeSlotsDataList.lastIndex) {
                    estimatedTime = slot.endTime
                    return@forEachIndexed
                }

                val nextSlot = busyTimeSlotsDataList[index + 1]
                // Рефакторил, может быть ошибка в этой проверке
                if (slot.endTime + tenMinsInMs < nextSlot.startTime) {
                    hasEnoughTime = slot.endTime + fifteenMinsInMs < nextSlot.startTime

                    estimatedTime = slot.endTime
                    return@forEachIndexed
                }
            }

            estimatedTime to hasEnoughTime
        }

        val roundedClosestTimeToTakeOrders = roundWithLowSeconds(closestTimeToTakeOrders)

        // Кофешоп закрыт на выходной
        return if (coffeeShopWorkTime == null || coffeeShopWorkTime.isClosed) {
            ClosestTimeToTakeOrderModel(null, IsCoffeeShopReadyToOrderReason.ShopClosed)
        // Не попадаем на время работы кофешопа
        } else if (roundedClosestTimeToTakeOrders !in coffeeShopWorkTime.startTime..coffeeShopWorkTime.endTime) {
            ClosestTimeToTakeOrderModel(null, IsCoffeeShopReadyToOrderReason.ShopClosed)
        // не больше часа от текущего времени
        } else if (roundedClosestTimeToTakeOrders >= currentTime + (60 * 60_000)) {
            ClosestTimeToTakeOrderModel(null, IsCoffeeShopReadyToOrderReason.TooManyOrders)
        // Все норм!!!
        } else {
            if (hasEnoughTimeBetweenOrders) {
                ClosestTimeToTakeOrderModel(roundedClosestTimeToTakeOrders, IsCoffeeShopReadyToOrderReason.ReadyToTake)
            } else {
                ClosestTimeToTakeOrderModel(roundedClosestTimeToTakeOrders, IsCoffeeShopReadyToOrderReason.OnlyShortOrder)
            }
        }
    }

    private fun calculateAvailableOrderTimeList(
        orderTimeStartsFrom: Long,
        maxOrderTime: Long,
        busyTimeSlots: List<BusyTimeSlotsData>,
        estimatedOrderFinishTimeInMinutes: Int,
    ): List<Long> {
        val estimatedOrderFinishTimeInMs = estimatedOrderFinishTimeInMinutes * 60_000L
        val available = mutableListOf<Long>()

        var tempTime = orderTimeStartsFrom

        while (tempTime + estimatedOrderFinishTimeInMs <= maxOrderTime) {
            val orderStart = tempTime
            val orderEnd = tempTime + estimatedOrderFinishTimeInMs

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

    /**
     * Находит ближайшую смену (рабочий интервал) кофейни и возвращает её начало и конец в миллисекундах с эпохи (UTC).
     *
     * @return WorkTimeModel где даты в формате epoch milliseconds, или null, если расписание не найдено
     */
    fun findShopsCurrentWorkTime(
        shopId: Long,
    ): WorkTimeModel? {
        val schedule = CoffeeShopTable
            .join(CoffeeShopScheduleTable, joinType = JoinType.LEFT, CoffeeShopTable.id, CoffeeShopScheduleTable.shopId)
            .select(CoffeeShopScheduleTable.columns)
            .where {
                CoffeeShopTable.id eq shopId
            }
            .map {
                Schedule(
                    dayOfWeek = it[CoffeeShopScheduleTable.dayOfWeek],
                    startTime = it[CoffeeShopScheduleTable.startTime],
                    endTime = it[CoffeeShopScheduleTable.endTime],
                    isClosed = it[CoffeeShopScheduleTable.isClosed],
                )
            }

        // Offset - 3, что может быть в будущем ошибкой. Хардкод часового пояса.
        val today = Clock.System.now()
            .toJavaInstant()
            .atOffset(ZoneOffset.ofHours(3))

        val todayDayOfWeek = today.dayOfWeek.value

        // Берем дни вчера, сегодня и завтра
        val targetDays = schedule.filter { s ->
            val diff = (s.dayOfWeek - todayDayOfWeek).mod(7)
            diff == 0 || diff == 1 || diff == 6  // 0 = сегодня, 1 = завтра, 6 = вчера
        }.sortedBy { it.dayOfWeek }

        return targetDays.mapNotNull {
            if (it.isClosed || it.startTime.isNullOrBlank() || it.endTime.isNullOrBlank())
                return@mapNotNull null

            val daysDifference = it.dayOfWeek - todayDayOfWeek
            val targetDay = if (daysDifference >= 0) {
                today.plusDays(daysDifference.toLong())
            } else {
                today.minusDays(daysDifference.absoluteValue.toLong())
            }

            val startTime = LocalTime.parse(it.startTime, DateTimeFormatter.ofPattern(HH_MM_PATTERN))
            val endTime = LocalTime.parse(it.endTime, DateTimeFormatter.ofPattern(HH_MM_PATTERN))

            val startDateTime = targetDay.toLocalDate().toEpochSecond(startTime, ZoneOffset.of(ZONE_ID)).times(1000)

            // Если endTime <= startTime → смена идёт через полночь (+1 день)
            val endDate = if (startTime > endTime && endTime != startTime) {
                targetDay.plusDays(1)
            } else {
                targetDay
            }

            val endDateTime = endDate.toLocalDate().toEpochSecond(endTime, ZoneOffset.of(ZONE_ID)).times(1000)

            WorkTimeModel(
                dayOfWeek = it.dayOfWeek,
                startTime = startDateTime,
                endTime = endDateTime,
                isClosed = false
            )
        }.firstNotNullOfOrNull {
            val todayInMs = today.toEpochSecond().times(1000)

            if (todayInMs > it.startTime && todayInMs < it.endTime) {
                WorkTimeModel(
                    dayOfWeek = it.dayOfWeek,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    isClosed = false
                )
            } else {
                null
            }
        } ?: schedule.first { it.dayOfWeek == todayDayOfWeek }.takeIf { !it.isClosed && !it.startTime.isNullOrBlank() && !it.endTime.isNullOrBlank() }?.let { currentDay(it) }
    }

    private fun currentDay(
        currentSchedule: Schedule
    ) : WorkTimeModel {
        val today = Clock.System.now()
            .toJavaInstant()
            .atOffset(ZoneOffset.ofHours(3))

        val startTime = LocalTime.parse(currentSchedule.startTime, DateTimeFormatter.ofPattern(HH_MM_PATTERN))
        val endTime = LocalTime.parse(currentSchedule.endTime, DateTimeFormatter.ofPattern(HH_MM_PATTERN))

        val startDateTime = today.toLocalDate().toEpochSecond(startTime, ZoneOffset.of(ZONE_ID)).times(1000)

        // Если endTime <= startTime → смена идёт через полночь (+1 день)
        val endDate = if (startTime > endTime && endTime != startTime) {
            today.plusDays(1)
        } else {
            today
        }

        val endDateTime = endDate.toLocalDate().toEpochSecond(endTime, ZoneOffset.of(ZONE_ID)).times(1000)

        return WorkTimeModel(
            dayOfWeek = currentSchedule.dayOfWeek,
            isClosed = currentSchedule.isClosed,
            startTime = startDateTime,
            endTime = endDateTime,
        )
    }

    /**
     * Фильтрует список временных меток так, чтобы между соседними элементами
     * разница была строго больше 3 минут. Это позволяет не показывать клиенту
     * слишком близкие варианты времени заказа. Каждая метка сдвигается вперёд
     * на время приготовления заказа — таким образом клиент видит время готовности,
     * а не время начала приготовления.
     */
    private fun filterByMinInterval(timestamps: List<Long>, estimatedOrderFinishTimeInMinutes: Int): List<Long> {
        val threeMinutesInMs = 3 * 60_000L
        val cookingTimeMs = estimatedOrderFinishTimeInMinutes * 60_000L
        return timestamps.fold(mutableListOf()) { acc, timestamp ->
            if (acc.isEmpty() || timestamp - (acc.last() - cookingTimeMs) > threeMinutesInMs) {
                acc.add(timestamp + cookingTimeMs)
            }
            acc
        }
    }

    private fun roundWithLowSeconds(closestTimeToTakeOrders: Long): Long {
        var localTime = LocalDateTime.ofEpochSecond(closestTimeToTakeOrders.div(1000), 0, ZoneOffset.ofHours(3))
        val secondsDifference = 60 - localTime.second
        if (secondsDifference < 5) {
            localTime = localTime.plusSeconds(secondsDifference.toLong())
        }

        return localTime.toEpochSecond(ZoneOffset.ofHours(3)).times(1000)
    }

    private data class Schedule(
        val startTime: String?,
        val endTime: String?,
        val dayOfWeek: Int,
        val isClosed: Boolean,
    )

    companion object {
        private const val ZONE_ID = "+03:00"
        private const val HH_MM_PATTERN = "HH:mm"
    }
}
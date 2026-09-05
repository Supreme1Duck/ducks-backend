package com.ducks.features.orders.data.repository

import com.ducks.features.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import com.ducks.features.orders.data.model.ClosestTimeToTakeOrderModel
import com.ducks.features.orders.data.model.IsCoffeeShopReadyToOrderReason
import com.ducks.features.orders.data.model.WorkTimeModel
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.util.ceilToMinute
import features.orders.data.model.BusyTimeSlotsData
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class FetchAvailableOrdersTimeListRepository {

    /**
     * Времена готовности, которые кофейня реально может обещать для заказа такой
     * длительности: свободны с учётом занятости, влезают целиком и заканчиваются
     * до закрытия смены. null — свободного времени нет вообще.
     *
     * Вызывать внутри транзакции.
     */
    fun availableFinishTimes(
        shopId: Long,
        estimatedOrderFinishTimeInMinutes: Int,
    ): List<Long>? {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        val busyTimeSlots = getAllBusyTimeSlots(shopId)

        val workTime = findShopsCurrentWorkTime(shopId)

        val closestTimeToTakeOrder =
            calculateClosestTimeToTakeOrder(
                busyTimeSlotsDataList = busyTimeSlots,
                coffeeShopWorkTime = workTime,
                currentTime = currentTime,
            ).closestTime
                ?: return null

        val maxOrderTime = minOf(currentTime + ORDER_MAX_END_TIME, workTime?.endTime ?: Long.MAX_VALUE)

        val availableOrderTimeList =
            calculateAvailableOrderTimeList(
                orderTimeStartsFrom = closestTimeToTakeOrder,
                maxOrderTime = maxOrderTime,
                busyTimeSlots = busyTimeSlots,
                estimatedOrderFinishTimeInMinutes = estimatedOrderFinishTimeInMinutes
            )

        return filterByMinInterval(availableOrderTimeList, estimatedOrderFinishTimeInMinutes)
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
                        // Приготовленный, но ещё не выданный заказ бариста уже не занимает,
                        // поэтому его слот освобождается по readyTime, а не по finishedTime.
                        (CoffeeOrdersTable.readyTime eq null) and
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
        currentTime: Long = Clock.System.now().toEpochMilliseconds(),
    ): ClosestTimeToTakeOrderModel {
        // Окно короче 5 минут не считаем окном вообще, окна короче 10 минут хватает только на быстрый заказ
        val fiveMinsInMs = 5 * 60_000
        val tenMinsInMs = 10 * 60_000

        val startTimeOfFirstTimeSlot = busyTimeSlotsDataList.firstOrNull()?.startTime

        // Если между заказами меньше 10 минут - выставляем флаг hasEnoughTimeBetweenOrders
        val (closestTimeToTakeOrders, hasEnoughTimeBetweenOrders) = if (busyTimeSlotsDataList.isEmpty()) {
            currentTime to true
        } else if (currentTime + fiveMinsInMs < startTimeOfFirstTimeSlot!!) {
            // до первого заказа больше 5 минут
            val hasEnoughTime = currentTime + tenMinsInMs < startTimeOfFirstTimeSlot

            currentTime to hasEnoughTime
        } else {
            // Берём конец первого слота, после которого есть окно больше 5 минут до следующего.
            // Если такого окна нет — берём конец последнего слота.
            val gapIndex = busyTimeSlotsDataList.indices.first { index ->
                index == busyTimeSlotsDataList.lastIndex ||
                        busyTimeSlotsDataList[index].endTime + fiveMinsInMs < busyTimeSlotsDataList[index + 1].startTime
            }

            val slot = busyTimeSlotsDataList[gapIndex]
            val nextSlot = busyTimeSlotsDataList.getOrNull(gapIndex + 1)
            val hasEnoughTime = nextSlot == null || slot.endTime + tenMinsInMs < nextSlot.startTime

            slot.endTime to hasEnoughTime
        }

        val roundedClosestTimeToTakeOrders = ceilToMinute(closestTimeToTakeOrders)

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
    fun findShopsCurrentWorkTime(shopId: Long): WorkTimeModel? {
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

        // Offset +3 — хардкод часового пояса Москвы
        val now = Clock.System.now().toJavaInstant().atOffset(ZoneOffset.ofHours(3))
        return calculateWorkTime(schedule, now)
    }

    fun findShopsCurrentWorkTime(shopIds: List<Long>): Map<Long, WorkTimeModel> {
        if (shopIds.isEmpty()) return emptyMap()

        val scheduleByShop = CoffeeShopScheduleTable
            .selectAll()
            .where { CoffeeShopScheduleTable.shopId inList shopIds }
            .groupBy({ it[CoffeeShopScheduleTable.shopId].value }) {
                Schedule(
                    dayOfWeek = it[CoffeeShopScheduleTable.dayOfWeek],
                    startTime = it[CoffeeShopScheduleTable.startTime],
                    endTime = it[CoffeeShopScheduleTable.endTime],
                    isClosed = it[CoffeeShopScheduleTable.isClosed],
                )
            }

        // Offset +3 — хардкод часового пояса Москвы, как и в одиночной версии.
        val now = Clock.System.now().toJavaInstant().atOffset(ZoneOffset.ofHours(3))

        return scheduleByShop.mapNotNull { (shopId, schedule) ->
            val workTime = calculateWorkTime(schedule, now) ?: return@mapNotNull null
            shopId to workTime
        }.toMap()
    }

    internal fun calculateWorkTime(schedule: List<Schedule>, now: OffsetDateTime): WorkTimeModel? {
        val candidateDays = listOf(now.minusDays(1), now, now.plusDays(1))

        val activeShift = candidateDays.firstNotNullOfOrNull { day ->
            val entry = schedule.firstOrNull { it.dayOfWeek == day.dayOfWeek.value } ?: return@firstNotNullOfOrNull null
            if (entry.isClosed || entry.startTime.isNullOrBlank() || entry.endTime.isNullOrBlank()) return@firstNotNullOfOrNull null

            val workTime = buildWorkTimeForDay(entry, day)
            val nowInMs = now.toEpochSecond() * 1000
            workTime.takeIf { nowInMs in it.startTime..it.endTime }
        }

        if (activeShift != null) return activeShift

        // Кофешоп не работает прямо сейчас — возвращаем ближайшую следующую смену (сегодня или завтра)
        val nowInMs = now.toEpochSecond() * 1000
        return listOf(now, now.plusDays(1)).firstNotNullOfOrNull { day ->
            schedule.firstOrNull { it.dayOfWeek == day.dayOfWeek.value }
                ?.takeIf { !it.isClosed && !it.startTime.isNullOrBlank() && !it.endTime.isNullOrBlank() }
                ?.let { buildWorkTimeForDay(it, day) }
                ?.takeIf { nowInMs < it.endTime }  // смена ещё не закончилась
        }
    }

    private fun buildWorkTimeForDay(schedule: Schedule, day: OffsetDateTime): WorkTimeModel {
        val startTime = LocalTime.parse(schedule.startTime, DateTimeFormatter.ofPattern(HH_MM_PATTERN))
        val endTime = LocalTime.parse(schedule.endTime, DateTimeFormatter.ofPattern(HH_MM_PATTERN))

        val startDateTime = day.toLocalDate().toEpochSecond(startTime, ZoneOffset.of(ZONE_ID)).times(1000)

        val endDate = if (startTime > endTime && endTime != startTime) {
            day.plusDays(1)
        } else {
            day
        }

        val endDateTime = endDate.toLocalDate().toEpochSecond(endTime, ZoneOffset.of(ZONE_ID)).times(1000)

        return WorkTimeModel(
            dayOfWeek = schedule.dayOfWeek,
            isClosed = schedule.isClosed,
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

    internal data class Schedule(
        val startTime: String?,
        val endTime: String?,
        val dayOfWeek: Int,
        val isClosed: Boolean,
    )

    companion object {
        // Заказ можно оформить не дальше чем на час вперёд.
        private const val ORDER_MAX_END_TIME = 60 * 60_000L

        private const val ZONE_ID = "+03:00"
        private const val HH_MM_PATTERN = "HH:mm"
    }
}
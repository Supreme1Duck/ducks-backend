package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import com.ducks.features.coffeeshops.database.mappers.mapToSellerCoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeShopsDataSource
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.seller.routings.request.shop.*
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeShopRepository(
    private val sellerCoffeeShopDataSource: SellerCoffeeShopsDataSource,
    private val coffeeShopImageRepository: CoffeeShopImageRepository,
    private val fetchAvailableOrdersTimeListRepository: FetchAvailableOrdersTimeListRepository,
    private val shopsClosestTimeService: CalculateCoffeeShopsOrdersTimeService,
) {

    suspend fun getShopDetails(shopId: Long): SellerCoffeeShopDetailsDTO {
        return newSuspendedTransaction {
            println("$shopId")

            CoffeeShopTable
                .join(
                    otherTable = CoffeeShopTechnicalPausesTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeShopTable.id,
                    otherColumn = CoffeeShopTechnicalPausesTable.coffeeShop,
                    additionalConstraint = {
                        (CoffeeShopTechnicalPausesTable.isActive eq true)
                    }
                )
                .selectAll()
                .where {
                    (CoffeeShopTable.id eq shopId)
                }.map {
                    val activeDaySchedule = fetchAvailableOrdersTimeListRepository.findShopsCurrentWorkTime(shopId)
                    val schedule = sellerCoffeeShopDataSource.fetchSchedule(shopId)

                    it.mapToSellerCoffeeShopDetailsDTO(activeDaySchedule, schedule)
                }.first()
        }
    }

    suspend fun updateShop(
        shopId: Long,
        request: UpdateCoffeeShopRequest,
    ) {
        sellerCoffeeShopDataSource.update(shopId, request)
    }

    suspend fun updateFreeTables(shopId: Long, freeTables: Int) {
        return newSuspendedTransaction {
            sellerCoffeeShopDataSource.updateFreeTables(shopId, freeTables)
        }
    }

    suspend fun addTechnicalPause(
        shopId: Long,
        startsAt: Long,
        endsAt: Long,
    ) {
        return newSuspendedTransaction {
            if (endsAt < System.currentTimeMillis() || endsAt <= startsAt) {
                throw DucksBadRequestError("Неверное время окончания паузы!")
            }

            val activePauseAlreadyExists = CoffeeShopTechnicalPausesTable
                .selectAll()
                .where { (CoffeeShopTechnicalPausesTable.coffeeShop eq shopId) and (CoffeeShopTechnicalPausesTable.isActive eq true) }
                .firstOrNull() != null

            if (activePauseAlreadyExists) {
                throw DucksBadRequestError("Мы пока не поддерживаем несколько активных пауз!")
            }

            if (!isActivePauseValid(shopId, startsAt, endsAt)) {
                throw DucksBadRequestError("Время паузы пересекается с одним из ваших заказов!")
            }

            CoffeeShopTechnicalPausesTable.insert {
                it[CoffeeShopTechnicalPausesTable.startsAt] = startsAt
                it[CoffeeShopTechnicalPausesTable.endsAt] = endsAt
                it[coffeeShop] = shopId
            }

            shopsClosestTimeService.invoke(shopId)
        }
    }

    // Проверяет пересекается ли время активных и pending заказов с новой паузой.
    private fun isActivePauseValid(
        shopId: Long,
        pauseStartsAt: Long,
        pauseEndsAt: Long,
    ): Boolean {
        val busyTimeSlots = fetchAvailableOrdersTimeListRepository.getAllBusyTimeSlots(shopId)

        busyTimeSlots.forEach { slot ->
            if (pauseStartsAt <= slot.endTime && slot.startTime <= pauseEndsAt) {
                return false
            }
        }

        return true
    }

    suspend fun deletePause(
        shopId: Long,
    ) {
        return newSuspendedTransaction {
            CoffeeShopTechnicalPausesTable
                .update(
                    where = {
                        (CoffeeShopTechnicalPausesTable.coffeeShop eq shopId) and (CoffeeShopTechnicalPausesTable.isActive eq true)
                    }
                ) {
                    it[isActive] = false
                }

            shopsClosestTimeService.invoke(shopId)
        }
    }

    suspend fun setSchedule(shopId: Long, schedule: SetCoffeeShopScheduleRequest) {
        try {
            newSuspendedTransaction {
                if (schedule.schedule.size != 7 && schedule.schedule.any { (it.value?.startTime?.length!! > 5 || it.value?.endTime?.length!! > 5) }) {
                    throw DucksBadRequestError("Неверный формат данных о расписании.")
                }

                CoffeeShopScheduleTable.deleteWhere {
                    CoffeeShopScheduleTable.shopId eq shopId
                }

                val scheduleMap = schedule.schedule.map {
                    mapDayOfWeek(it.key) to it.value
                }

                CoffeeShopScheduleTable.batchInsert(scheduleMap) { (dayOfWeek, scheduleData) ->
                    this[CoffeeShopScheduleTable.shopId] = shopId
                    this[CoffeeShopScheduleTable.dayOfWeek] = dayOfWeek

                    this[CoffeeShopScheduleTable.isClosed] = scheduleData == null

                    this[CoffeeShopScheduleTable.startTime] = scheduleData?.startTime
                    this[CoffeeShopScheduleTable.endTime] = scheduleData?.endTime
                }
            }
        } catch (e: DucksBadRequestError) {
            throw e
        } catch (e: Exception) {
            throw DucksBadRequestError("Ошибка при добавлении расписания - ${e.stackTrace}")
        }
    }

    private fun mapDayOfWeek(entry: String): Int {
        return when (entry) {
            MONDAY_KEY -> 1
            TUESDAY_KEY -> 2
            WEDNESDAY_KEY -> 3
            THURSDAY_KEY -> 4
            FRIDAY_KEY -> 5
            SATURDAY_KEY -> 6
            SUNDAY_KEY -> 7
            else -> throw IllegalArgumentException()
        }
    }
}
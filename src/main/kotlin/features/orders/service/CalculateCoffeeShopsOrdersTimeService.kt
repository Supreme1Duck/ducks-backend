package com.ducks.features.orders.service

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.data.model.ClosestTimeToTakeOrderModel
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import com.ducks.service.MinuteChangeNotifierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Пересчитывает актуальное ближайшее время принятия заказа кофешопом
 */
class CalculateCoffeeShopsOrdersTimeService(
    private val fetchAvailableOrdersTimeListRepository: FetchAvailableOrdersTimeListRepository,
    private val minuteChangeNotifierService: MinuteChangeNotifierService,
) {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun initialize() {
        scope.launch {
            while (true) {
                updateTime()

                delay(15_000)
            }
        }

        minuteChangeNotifierService.observe()
            .onEach {
                updateTime()
            }.launchIn(scope)
    }

    operator fun invoke(coffeeShopId: Long) {
        scope.launch {
            updateShopsClosestTimeToTakeOrders(coffeeShopId)
        }
    }

    private suspend fun updateTime() {
        newSuspendedTransaction {
            val allCoffeeShopIds = CoffeeShopTable
                .select(CoffeeShopTable.id)
                .where {
                    CoffeeShopTable.isShown eq true
                }
                .map {
                    it[CoffeeShopTable.id].value
                }

            allCoffeeShopIds.forEach {
                invoke(it)
            }
        }
    }

    private suspend fun updateShopsClosestTimeToTakeOrders(coffeeShopId: Long) {
        newSuspendedTransaction {
            val allBusyTimeSlots = fetchAvailableOrdersTimeListRepository.getAllBusyTimeSlots(coffeeShopId)

            val workTime = fetchAvailableOrdersTimeListRepository.findShopsCurrentWorkTime(coffeeShopId)

            val closestTimeToTakeOrders =
                fetchAvailableOrdersTimeListRepository.calculateClosestTimeToTakeOrder(allBusyTimeSlots, workTime)

            updateDB(coffeeShopId = coffeeShopId, closestTimeToTakeOrders = closestTimeToTakeOrders)
        }
    }

    private fun updateDB(
        coffeeShopId: Long,
        closestTimeToTakeOrders: ClosestTimeToTakeOrderModel?,
    ) {
        CoffeeShopTable.update(
            where = {
                CoffeeShopTable.id eq coffeeShopId
            }
        ) {
            it[CoffeeShopTable.closestTimeToTakeOrders] = closestTimeToTakeOrders?.closestTime
            it[CoffeeShopTable.canTakeOrdersReason] = closestTimeToTakeOrders?.reason?.value
        }
    }
}
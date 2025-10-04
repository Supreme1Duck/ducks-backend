package com.ducks.features.orders.service

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Пересчитывает актуальное ближайшее время принятия заказа кофешопом
 */
class CalculateCoffeeShopsOrdersTimeService(
    private val fetchAvailableOrdersTimeListRepository: FetchAvailableOrdersTimeListRepository,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            while (true) {
                delay(60_000)
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
        }
    }

    operator fun invoke(coffeeShopId: Long) {
        scope.launch {
            updateShopsClosestTimeToTakeOrders(coffeeShopId)
        }
    }

    private suspend fun updateShopsClosestTimeToTakeOrders(coffeeShopId: Long) {
        newSuspendedTransaction {
            val allActiveOrdersEstimatedFinishTime = fetchAvailableOrdersTimeListRepository.getAllBusyTimeSlots(coffeeShopId)

            val closestTimeToTakeOrders = fetchAvailableOrdersTimeListRepository.calculateClosestTimeToTakeOrder(allActiveOrdersEstimatedFinishTime)

            updateDB(coffeeShopId = coffeeShopId, closestTimeToTakeOrders = closestTimeToTakeOrders)
        }
    }

    private fun updateDB(
        coffeeShopId: Long,
        closestTimeToTakeOrders: Long?,
    ) {
        CoffeeShopTable.update(
            where = {
                CoffeeShopTable.id eq coffeeShopId
            }
        ) {
            it[CoffeeShopTable.closestTimeToTakeOrders] = closestTimeToTakeOrders
        }
    }
}
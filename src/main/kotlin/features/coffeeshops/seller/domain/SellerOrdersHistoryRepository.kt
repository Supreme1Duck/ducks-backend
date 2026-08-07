package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.seller.data.model.SellerDayOrderDTO
import com.ducks.features.coffeeshops.seller.data.model.SellerOrderDetailsDTO
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.database.toOrderProductDTO
import com.ducks.features.orders.database.toOrderStatus
import com.ducks.features.user.database.UserTable
import com.ducks.util.APP_ZONE_OFFSET
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

// Просмотр заказов кофейни продавцом: список за день и детали одного заказа.
class SellerOrdersHistoryRepository {

    // Границы дня считаются в UTC+3 (часовой пояс приложения).
    suspend fun getOrdersByDay(shopId: Long, day: LocalDate): List<SellerDayOrderDTO> {
        return newSuspendedTransaction {
            val dayStart = day.atStartOfDay().toInstant(APP_ZONE_OFFSET).toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay().toInstant(APP_ZONE_OFFSET).toEpochMilli()

            val orders = CoffeeOrdersTable
                .join(
                    otherTable = UserTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeOrdersTable.userId,
                    otherColumn = UserTable.id,
                )
                .select(CoffeeOrdersTable.columns + UserTable.phoneNumber)
                .where {
                    (CoffeeOrdersTable.coffeeShop eq shopId) and
                            (CoffeeOrdersTable.createdTime greaterEq dayStart) and
                            (CoffeeOrdersTable.createdTime less dayEnd)
                }
                .orderBy(CoffeeOrdersTable.createdTime, SortOrder.DESC)
                .map {
                    SellerDayOrderDTO(
                        id = it[CoffeeOrdersTable.id].value,
                        createdAt = it[CoffeeOrdersTable.createdTime],
                        userPhoneNumber = it[UserTable.phoneNumber],
                        status = it.toOrderStatus().value,
                        // Будет заполнено дальше.
                        productsCount = 0,
                        comment = it[CoffeeOrdersTable.comment],
                        isTakeaway = it[CoffeeOrdersTable.isTakeaway],
                        price = it[CoffeeOrdersTable.totalPrice],
                    )
                }

            if (orders.isEmpty()) {
                return@newSuspendedTransaction orders
            }

            val quantitySum = CoffeeOrderedProductsTable.quantity.sum()
            val productsCounts = CoffeeOrderedProductsTable
                .select(CoffeeOrderedProductsTable.orderId, quantitySum)
                .where { CoffeeOrderedProductsTable.orderId inList orders.map { it.id } }
                .groupBy(CoffeeOrderedProductsTable.orderId)
                .associate { it[CoffeeOrderedProductsTable.orderId].value to (it[quantitySum] ?: 0) }

            orders.map { order ->
                order.copy(productsCount = productsCounts[order.id] ?: 0)
            }
        }
    }

    suspend fun getOrderDetails(shopId: Long, orderId: Long): SellerOrderDetailsDTO? {
        return newSuspendedTransaction {
            val order = CoffeeOrdersTable
                .join(
                    otherTable = UserTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeOrdersTable.userId,
                    otherColumn = UserTable.id,
                )
                .select(CoffeeOrdersTable.columns + UserTable.phoneNumber)
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map { it.toSellerOrderDetailsDTO() }
                .firstOrNull() ?: return@newSuspendedTransaction null

            val products = CoffeeOrderedProductsTable
                .selectAll()
                .where { CoffeeOrderedProductsTable.orderId eq orderId }
                .map { it.toOrderProductDTO() }

            order.copy(products = products)
        }
    }

    private fun ResultRow.toSellerOrderDetailsDTO() = SellerOrderDetailsDTO(
        id = this[CoffeeOrdersTable.id].value,
        createdAt = this[CoffeeOrdersTable.createdTime],
        acceptedAt = this[CoffeeOrdersTable.acceptedTime],
        readyAt = this[CoffeeOrdersTable.readyTime],
        finishedAt = this[CoffeeOrdersTable.finishedTime],
        estimatedFinishTime = this[CoffeeOrdersTable.estimatedFinishTime],
        userPhoneNumber = this[UserTable.phoneNumber],
        status = this.toOrderStatus().value,
        comment = this[CoffeeOrdersTable.comment],
        cancelledMessage = this[CoffeeOrdersTable.cancelledMessage],
        isToTime = this[CoffeeOrdersTable.isToTime],
        isTakeaway = this[CoffeeOrdersTable.isTakeaway],
        timeToCookInMinutes = this[CoffeeOrdersTable.timeToCookInMinutes],
        // Будут заполнены дальше.
        products = emptyList(),
        price = this[CoffeeOrdersTable.price],
        tips = this[CoffeeOrdersTable.tips],
        totalPrice = this[CoffeeOrdersTable.totalPrice],
    )
}

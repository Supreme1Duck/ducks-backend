package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.database.UserTable
import com.ducks.service.PushNotificationService
import com.ducks.service.PushType
import com.ducks.util.DucksBadRequestError
import io.ktor.server.application.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.ktor.ext.inject

class SellerOrdersRepository(
    application: Application,
) {

    private val calculateCoffeeShopsOrdersTimeService by application.inject<CalculateCoffeeShopsOrdersTimeService>()
    private val pushNotificationService by application.inject<PushNotificationService>()

    suspend fun acceptOrder(orderId: Long, shopId: Long) {
        val fcmToken = newSuspendedTransaction {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            val (isOrderCorrect, closestTimeToTakeOrders) = getAdditionalInfo(orderId, shopId)
                ?: throw DucksBadRequestError("Попытка принять несуществующий заказ")

            if (!isOrderCorrect) {
                throw DucksBadRequestError("Попытка принять невалидный заказ!")
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[acceptedTime] = currentTime
            }

            getFcmToken(orderId)
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)

        fcmToken?.let {
            pushNotificationService.send(
                fcmToken = it,
                title = "Заказ принят",
                body = "Ваш заказ принят и скоро будет готов.",
                type = PushType.ORDER_ACCEPTED,
                orderId = orderId,
            )
        }
    }

    suspend fun cancelBySeller(orderId: Long, shopId: Long, message: String?) {
        val fcmToken = newSuspendedTransaction {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            val isOrderCancellable = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map {
                    it[CoffeeOrdersTable.finishedTime] == null
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка отменить несуществующий заказ!")

            if (!isOrderCancellable) {
                throw DucksBadRequestError("Попытка отменить невалидный заказ!")
            }

            CoffeeOrdersTable.update(
                where = {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
            ) {
                it[finishedTime] = currentTime
                it[isCancelledBySeller] = true
                it[cancelledMessage] = message
            }

            getFcmToken(orderId)
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)

        fcmToken?.let {
            pushNotificationService.send(
                fcmToken = it,
                title = "Заказ отменён",
                body = if (message.isNullOrBlank()) "Ваш заказ был отменён." else "Ваш заказ был отменён: $message",
                type = PushType.ORDER_CANCELLED_BY_SELLER,
                orderId = orderId,
            )
        }
    }

    // Заказ приготовлен и готов к выдаче (принят -> готов).
    suspend fun markOrderReady(orderId: Long, shopId: Long) {
        val fcmToken = newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val order = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка пометить готовым несуществующий заказ!")

            if (order[CoffeeOrdersTable.acceptedTime] == null) {
                throw DucksBadRequestError("Нельзя пометить готовым непринятый заказ!")
            }
            if (order[CoffeeOrdersTable.finishedTime] != null) {
                throw DucksBadRequestError("Попытка пометить готовым уже завершённый заказ!")
            }
            if (order[CoffeeOrdersTable.readyTime] != null) {
                throw DucksBadRequestError("Заказ уже готов!")
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[readyTime] = currentTime
            }

            getFcmToken(orderId)
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)

        fcmToken?.let {
            pushNotificationService.send(
                fcmToken = it,
                title = "Заказ готов",
                body = "Ваш заказ готов, можете забирать!",
                type = PushType.ORDER_READY,
                orderId = orderId,
            )
        }
    }

    // Заказ выдан клиенту (готов -> выдан).
    suspend fun giveOutOrder(orderId: Long, shopId: Long) {
        val fcmToken = newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val order = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка выдать несуществующий заказ!")

            if (order[CoffeeOrdersTable.finishedTime] != null) {
                throw DucksBadRequestError("Попытка выдать уже завершённый заказ!")
            }
            if (order[CoffeeOrdersTable.readyTime] == null) {
                throw DucksBadRequestError("Нельзя выдать неготовый заказ!")
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[finishedTime] = currentTime
            }

            getFcmToken(orderId)
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)

        fcmToken?.let {
            pushNotificationService.send(
                fcmToken = it,
                title = "Заказ выдан",
                body = "Спасибо за заказ! Приятного аппетита.",
                type = PushType.ORDER_GIVEN_OUT,
                orderId = orderId,
            )
        }
    }

    // Заказ был готов, но клиент его не забрал (готов -> не забран).
    suspend fun markOrderNotPickedUp(orderId: Long, shopId: Long) {
        val fcmToken = newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val order = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка завершить несуществующий заказ!")

            if (order[CoffeeOrdersTable.finishedTime] != null) {
                throw DucksBadRequestError("Попытка завершить уже завершённый заказ!")
            }
            if (order[CoffeeOrdersTable.readyTime] == null) {
                throw DucksBadRequestError("Нельзя пометить незабранным неготовый заказ!")
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[finishedTime] = currentTime
                it[isNotPickedUp] = true
            }

            getFcmToken(orderId)
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)

        fcmToken?.let {
            pushNotificationService.send(
                fcmToken = it,
                title = "Заказ не забран",
                body = "Вы не забрали свой заказ.",
                type = PushType.ORDER_NOT_PICKED_UP,
                orderId = orderId,
            )
        }
    }

    private fun getFcmToken(orderId: Long): String? {
        return CoffeeOrdersTable
            .join(UserTable, JoinType.LEFT, CoffeeOrdersTable.userId, UserTable.id)
            .select(UserTable.fcmToken)
            .where { CoffeeOrdersTable.id eq orderId }
            .map { it[UserTable.fcmToken] }
            .firstOrNull()
    }

    private fun getAdditionalInfo(orderId: Long, shopId: Long): Pair<Boolean, Long?>? {
        return CoffeeOrdersTable
            .join(CoffeeShopTable, JoinType.LEFT, CoffeeShopTable.id, CoffeeOrdersTable.coffeeShop)
            .select(CoffeeOrdersTable.acceptedTime, CoffeeShopTable.closestTimeToTakeOrders, CoffeeOrdersTable.finishedTime)
            .where {
                (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
            }
            .map {
                val isOrderCorrect =
                    it[CoffeeOrdersTable.acceptedTime] == null && it[CoffeeOrdersTable.finishedTime] == null

                isOrderCorrect to it[CoffeeShopTable.closestTimeToTakeOrders]
            }
            .firstOrNull()
    }
}
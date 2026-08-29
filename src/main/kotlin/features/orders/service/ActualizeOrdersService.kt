package com.ducks.features.orders.service

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.user.database.UserTable
import com.ducks.service.MinuteChangeNotifierService
import com.ducks.service.PushNotificationService
import com.ducks.service.PushType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ActualizeOrdersService(
    private val changeNotifierService: MinuteChangeNotifierService,
    private val pushNotificationService: PushNotificationService,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    operator fun invoke() {
        changeNotifierService.observe()
            .onEach {
                newSuspendedTransaction {
                    val currentTime = Clock.System.now().toEpochMilliseconds()

                    cancelNotAcceptedOrders(currentTime)
                    notifySellersAboutLateOrders(currentTime)
                }
            }
            .launchIn(coroutineScope)
    }

    // Отменяет непринятые заказы у которых истёк estimatedFinishTime
    private fun cancelNotAcceptedOrders(currentTime: Long) {
        val expiredOrders = CoffeeOrdersTable
            .join(
                otherTable = UserTable,
                joinType = org.jetbrains.exposed.v1.core.JoinType.LEFT,
                onColumn = CoffeeOrdersTable.userId,
                otherColumn = UserTable.id,
            )
            .select(CoffeeOrdersTable.id, UserTable.fcmToken)
            .where { isNotAccepted(currentTime) }
            .map { it[CoffeeOrdersTable.id].value to it[UserTable.fcmToken] }

        if (expiredOrders.isEmpty()) return

        CoffeeOrdersTable.update(
            where = { isNotAccepted(currentTime) }
        ) {
            it[finishedTime] = currentTime
            it[isExpired] = true
            it[isCancelledBySeller] = true
        }

        expiredOrders.forEach { (orderId, fcmToken) ->
            if (fcmToken != null) {
                pushNotificationService.send(
                    fcmToken = fcmToken,
                    title = "Заказ отменён",
                    body = "Ваш заказ не был принят вовремя и был автоматически отменён.",
                    type = PushType.ORDER_EXPIRED,
                    orderId = orderId,
                )
            }
        }
    }

    /**
     * Принятый заказ, который через минуту после обещанного времени так и не помечен
     * готовым, продолжает занимать баристу в глазах клиента, но из очереди уже выпал.
     * Продавцу напоминаем один раз: либо он закрывает заказ либо помечает не забранным.
     */
    private fun notifySellersAboutLateOrders(currentTime: Long) {
        val lateOrders = CoffeeOrdersTable
            .join(
                otherTable = CoffeeShopTable,
                joinType = org.jetbrains.exposed.v1.core.JoinType.LEFT,
                onColumn = CoffeeOrdersTable.coffeeShop,
                otherColumn = CoffeeShopTable.id,
            )
            .select(CoffeeOrdersTable.id, CoffeeShopTable.fcmToken)
            .where { isLate(currentTime) }
            .map { it[CoffeeOrdersTable.id].value to it[CoffeeShopTable.fcmToken] }

        if (lateOrders.isEmpty()) return

        CoffeeOrdersTable.update(
            where = { CoffeeOrdersTable.id inList lateOrders.map { (orderId, _) -> orderId } }
        ) {
            it[isLateNotified] = true
        }

        lateOrders.forEach { (orderId, fcmToken) ->
            if (fcmToken != null) {
                pushNotificationService.sendToSeller(
                    fcmToken = fcmToken,
                    title = "Заказ просрочен",
                    body = "Обещанное клиенту время готовности прошло — проверьте готовность.",
                    type = PushType.ORDER_LATE,
                    orderId = orderId,
                )
            }
        }
    }

    private fun isNotAccepted(currentTime: Long): Op<Boolean> {
        return (CoffeeOrdersTable.acceptedTime eq null) and
                (CoffeeOrdersTable.finishedTime eq null) and
                (CoffeeOrdersTable.estimatedFinishTime less currentTime)
    }

    private fun isLate(currentTime: Long): Op<Boolean> {
        return CoffeeOrdersTable.acceptedTime.isNotNull() and
                (CoffeeOrdersTable.readyTime eq null) and
                (CoffeeOrdersTable.finishedTime eq null) and
                (CoffeeOrdersTable.isLateNotified eq false) and
                (CoffeeOrdersTable.estimatedFinishTime less (currentTime - LATE_ORDER_DELAY_MS))
    }

    private companion object {
        // Напоминаем через минуту после обещанного времени готовности, не раньше.
        const val LATE_ORDER_DELAY_MS = 60_000L
    }
}

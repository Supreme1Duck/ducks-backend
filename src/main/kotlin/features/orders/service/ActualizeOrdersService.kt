package com.ducks.features.orders.service

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

    // Отменяет непринятые заказы у которых истёк estimatedFinishTime
    operator fun invoke() {
        changeNotifierService.observe()
            .onEach {
                newSuspendedTransaction {
                    val currentTime = Clock.System.now().toEpochMilliseconds()

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

                    if (expiredOrders.isNotEmpty()) {
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
                }
            }
            .launchIn(coroutineScope)
    }

    private fun isNotAccepted(currentTime: Long): Op<Boolean> {
        return (CoffeeOrdersTable.acceptedTime eq null) and
                (CoffeeOrdersTable.finishedTime eq null) and
                (CoffeeOrdersTable.estimatedFinishTime less currentTime)
    }
}

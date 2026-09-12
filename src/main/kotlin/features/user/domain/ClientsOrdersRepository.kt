package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import com.ducks.features.orders.database.orderedProductUnitPrice
import com.ducks.features.orders.database.toOrderStatus
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.data.dto.ActiveOrderDTO
import com.ducks.features.user.data.dto.ActiveOrderProductDTO
import com.ducks.features.user.data.dto.ClientOrderDTO
import com.ducks.features.user.data.dto.ClientOrderProductDTO
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

class ClientsOrdersRepository(
    application: Application,
) {
    private val calculateCoffeeShopsOrdersTimeService by application.inject<CalculateCoffeeShopsOrdersTimeService>()
    private val pushNotificationService by application.inject<PushNotificationService>()

    suspend fun getActiveOrder(userId: Long): ActiveOrderDTO? {
        return newSuspendedTransaction {
            val activeOrder = CoffeeOrdersTable
                .join(
                    otherTable = CoffeeShopTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeShopTable.id,
                    otherColumn = CoffeeOrdersTable.coffeeShop
                )
                .select(CoffeeOrdersTable.columns + CoffeeShopTable.name + CoffeeShopTable.address)
                .where {
                    (CoffeeOrdersTable.userId eq userId) and
                            (CoffeeOrdersTable.finishedTime eq null)
                }
                .map {
                    ActiveOrderDTO(
                        id = it[CoffeeOrdersTable.id].value,
                        shopId = it[CoffeeOrdersTable.coffeeShop].value,
                        shopName = it[CoffeeShopTable.name],
                        shopAddress = it[CoffeeShopTable.address],
                        isAccepted = it[CoffeeOrdersTable.acceptedTime] != null,
                        isReady = it[CoffeeOrdersTable.readyTime] != null,
                        estimatedFinishTime = it[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
                        createdTime = it[CoffeeOrdersTable.createdTime],
                        isTakeaway = it[CoffeeOrdersTable.isTakeaway],
                        // Будут заполнены дальше.
                        products = emptyList(),
                        price = it[CoffeeOrdersTable.totalPrice],
                    )
                }
                .firstOrNull()

            if (activeOrder != null) {
                val products = CoffeeOrderedProductsTable
                    .select(
                        CoffeeOrderedProductsTable.productId,
                        CoffeeOrderedProductsTable.productName,
                        CoffeeOrderedProductsTable.imageUrl,
                        CoffeeOrderedProductsTable.price,
                        CoffeeOrderedProductsTable.quantity,
                        CoffeeOrderedProductsTable.constructors,
                        CoffeeOrderedProductsTable.selectedSize,
                    )
                    .where { CoffeeOrderedProductsTable.orderId eq activeOrder.id }
                    .map {
                        ActiveOrderProductDTO(
                            id = it[CoffeeOrderedProductsTable.productId],
                            name = it[CoffeeOrderedProductsTable.productName],
                            imageUrl = it[CoffeeOrderedProductsTable.imageUrl],
                            quantity = it[CoffeeOrderedProductsTable.quantity],
                            unitPrice = it.orderedProductUnitPrice(),
                            price = it[CoffeeOrderedProductsTable.price] ?: 0.toBigDecimal(),
                            constructors = it[CoffeeOrderedProductsTable.constructors]?.toActiveOrderConstructors(),
                            size = it[CoffeeOrderedProductsTable.selectedSize].toActiveOrderSize(),
                        )
                    }

                activeOrder.copy(products = products)
            } else {
                null
            }
        }
    }

    suspend fun getOrders(userId: Long): List<ClientOrderDTO> {
        return newSuspendedTransaction {
            val orders = CoffeeOrdersTable
                .join(CoffeeShopTable, joinType = JoinType.LEFT, CoffeeOrdersTable.coffeeShop, CoffeeShopTable.id)
                .select(CoffeeOrdersTable.columns + CoffeeShopTable.name + CoffeeShopTable.address)
                .where { CoffeeOrdersTable.userId eq userId }
                .map {
                    ClientOrderDTO(
                        id = it[CoffeeOrdersTable.id].value,
                        shopId = it[CoffeeOrdersTable.coffeeShop].value,
                        shopName = it[CoffeeShopTable.name],
                        shopAddress = it[CoffeeShopTable.address],
                        finishedAt = it[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
                        products = emptyList(),
                        comment = it[CoffeeOrdersTable.comment],
                        isTakeaway = it[CoffeeOrdersTable.isTakeaway],
                        status = it.toOrderStatus().value,
                        price = it[CoffeeOrdersTable.totalPrice],
                    )
                }

            val products = CoffeeOrderedProductsTable
                .select(
                    CoffeeOrderedProductsTable.orderId,
                    CoffeeOrderedProductsTable.productId,
                    CoffeeOrderedProductsTable.productName,
                    CoffeeOrderedProductsTable.imageUrl,
                    CoffeeOrderedProductsTable.selectedSize,
                    CoffeeOrderedProductsTable.constructors,
                    CoffeeOrderedProductsTable.quantity,
                    CoffeeOrderedProductsTable.price,
                )
                .where { CoffeeOrderedProductsTable.orderId inList orders.map { it.id } }
                .groupBy { it[CoffeeOrderedProductsTable.orderId].value }

            orders.map { order ->
                order.copy(
                    products = products[order.id]?.map {
                        ClientOrderProductDTO(
                            id = it[CoffeeOrderedProductsTable.productId],
                            name = it[CoffeeOrderedProductsTable.productName],
                            imageUrl = it[CoffeeOrderedProductsTable.imageUrl],
                            size = it[CoffeeOrderedProductsTable.selectedSize].toClientOrderSize(),
                            constructors = it[CoffeeOrderedProductsTable.constructors]?.toClientOrderConstructors(),
                            quantity = it[CoffeeOrderedProductsTable.quantity],
                            unitPrice = it.orderedProductUnitPrice(),
                            price = it[CoffeeOrderedProductsTable.price] ?: java.math.BigDecimal.ZERO,
                        )
                    } ?: emptyList()
                )
            }
        }
    }

    suspend fun getOrder(orderId: Long, userId: Long): ClientOrderDTO? {
        return newSuspendedTransaction {
            val order = CoffeeOrdersTable
                .join(CoffeeShopTable, joinType = JoinType.LEFT, CoffeeOrdersTable.coffeeShop, CoffeeShopTable.id)
                .select(CoffeeOrdersTable.columns + CoffeeShopTable.name + CoffeeShopTable.address)
                .where { (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.userId eq userId) }
                .map {
                    ClientOrderDTO(
                        id = it[CoffeeOrdersTable.id].value,
                        shopId = it[CoffeeOrdersTable.coffeeShop].value,
                        shopName = it[CoffeeShopTable.name],
                        shopAddress = it[CoffeeShopTable.address],
                        finishedAt = it[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
                        products = emptyList(),
                        comment = it[CoffeeOrdersTable.comment],
                        cancelledMessage = it[CoffeeOrdersTable.cancelledMessage],
                        isTakeaway = it[CoffeeOrdersTable.isTakeaway],
                        status = it.toOrderStatus().value,
                        price = it[CoffeeOrdersTable.totalPrice],
                    )
                }
                .firstOrNull() ?: return@newSuspendedTransaction null

            val products = CoffeeOrderedProductsTable
                .select(
                    CoffeeOrderedProductsTable.orderId,
                    CoffeeOrderedProductsTable.productId,
                    CoffeeOrderedProductsTable.productName,
                    CoffeeOrderedProductsTable.imageUrl,
                    CoffeeOrderedProductsTable.selectedSize,
                    CoffeeOrderedProductsTable.constructors,
                    CoffeeOrderedProductsTable.quantity,
                    CoffeeOrderedProductsTable.price,
                )
                .where { CoffeeOrderedProductsTable.orderId eq orderId }
                .map {
                    ClientOrderProductDTO(
                        id = it[CoffeeOrderedProductsTable.productId],
                        name = it[CoffeeOrderedProductsTable.productName],
                        imageUrl = it[CoffeeOrderedProductsTable.imageUrl],
                        size = it[CoffeeOrderedProductsTable.selectedSize].toClientOrderSize(),
                        constructors = it[CoffeeOrderedProductsTable.constructors]?.toClientOrderConstructors(),
                        quantity = it[CoffeeOrderedProductsTable.quantity],
                        unitPrice = it.orderedProductUnitPrice(),
                        price = it[CoffeeOrderedProductsTable.price] ?: java.math.BigDecimal.ZERO,
                    )
                }

            order.copy(products = products)
        }
    }

    // Отменить можно только заказ, который продавец ещё не принял.
    suspend fun cancelOrder(orderId: Long, clientId: Long) {
        val (coffeeShopId, sellerFcmToken) = newSuspendedTransaction {
            val order = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and
                            (CoffeeOrdersTable.userId eq clientId)
                }
                .firstOrNull()
                ?: throw DucksBadRequestError("Попытка отменить невалидный заказ")

            if (order[CoffeeOrdersTable.acceptedTime] != null) {
                throw DucksBadRequestError("Заказ уже принят!")
            }
            if (order[CoffeeOrdersTable.finishedTime] != null) {
                throw DucksBadRequestError("Заказ уже завершён!")
            }

            val currentTime = Clock.System.now().toEpochMilliseconds()

            // Условия дублируются в самом update: между чтением строки и записью заказ
            // могли принять или отменить (продавец, автоотмена по истечении времени).
            val updatedRows = CoffeeOrdersTable
                .update(
                    where = {
                        (CoffeeOrdersTable.id eq orderId) and
                                (CoffeeOrdersTable.userId eq clientId) and
                                (CoffeeOrdersTable.acceptedTime eq null) and
                                (CoffeeOrdersTable.finishedTime eq null)
                    }
                ) {
                    it[isCancelledByClient] = true
                    it[finishedTime] = currentTime
                }

            if (updatedRows == 0) {
                throw DucksBadRequestError("Статус заказа успел измениться, отменить его уже нельзя")
            }

            val coffeeShopId = order[CoffeeOrdersTable.coffeeShop].value

            coffeeShopId to getShopFcmToken(coffeeShopId)
        }

        // Вне транзакции: сервис считает время в своей корутине и должен видеть
        // уже закоммиченную отмену.
        calculateCoffeeShopsOrdersTimeService.invoke(coffeeShopId)

        sellerFcmToken?.let {
            pushNotificationService.sendToSeller(
                fcmToken = it,
                title = "Заказ отменён",
                body = "Заказ #$orderId был отменён, поскольку не был принят.",
                type = PushType.ORDER_CANCELLED_BY_CLIENT,
                orderId = orderId,
            )
        }
    }

    private fun getShopFcmToken(shopId: Long): String? {
        return CoffeeShopTable
            .select(CoffeeShopTable.fcmToken)
            .where { CoffeeShopTable.id eq shopId }
            .map { it[CoffeeShopTable.fcmToken] }
            .firstOrNull()
    }

    private fun CoffeeProductSizeDTO.toClientOrderSize() = ClientOrderProductDTO.Size(
        id = id,
        sizeName = sizeName,
        sizeValue = sizeValue,
        price = price,
    )

    private fun List<OrderedProductConstructorDBModel>.toClientOrderConstructors() = map {
        ClientOrderProductDTO.Constructor(id = it.id, name = it.name, price = it.price)
    }

    private fun CoffeeProductSizeDTO.toActiveOrderSize() = ActiveOrderProductDTO.Size(
        id = id,
        sizeName = sizeName,
        sizeValue = sizeValue,
        price = price,
    )

    private fun List<OrderedProductConstructorDBModel>.toActiveOrderConstructors() = map {
        ActiveOrderProductDTO.Constructor(id = it.id, name = it.name, price = it.price)
    }
}
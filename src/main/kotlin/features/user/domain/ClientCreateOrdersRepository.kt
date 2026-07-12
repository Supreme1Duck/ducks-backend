package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.client.routings.request.CreateOrderRequest
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeProductsWithConstructorsTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.database.UserTable
import com.ducks.service.PushNotificationService
import com.ducks.util.DucksBadRequestError
import io.ktor.server.application.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.koin.ktor.ext.inject
import java.math.BigDecimal

class ClientCreateOrdersRepository(
    application: Application,
) {

    private val calculateCoffeeShopsOrdersTimeService by application.inject<CalculateCoffeeShopsOrdersTimeService>()
    private val pushNotificationService by application.inject<PushNotificationService>()

    suspend fun createOrder(
        request: CreateOrderRequest,
        clientPhoneNumber: String,
    ) {
        val sellerFcmToken = newSuspendedTransaction {
            val clientId = getUserIdByPhone(clientPhoneNumber)

            // Получаем полный список продуктов с дубликатами если их несколько.
            val allProducts = buildList {
                request.products.forEach { request ->
                    repeat(request.quantity ?: 1) {
                        add(request)
                    }
                }
            }

            if (userHasActiveOrder(clientId)) {
                throw DucksBadRequestError("Вы не можете создать новый заказ, когда у вас есть активный заказ")
            }

            val (estimatedTimeToFinish, minutesToCookAllProducts) = calculateOrderFinishTimeMs(
                request.shopId,
                allProducts.map { it.productId },
            )

            if (estimatedTimeToFinish == null) {
                throw DucksBadRequestError("У кофешопа нет свободного времени для принятия заказа.")
            }

            // Если посчитанное время окончания заказа отличается от времени выбранного клиентом более чем на 2 минуты, выбрасываем ошибку.
            if (estimatedTimeToFinish > request.estimatedTimeToFinish + 2.times(60_000)) {
                throw DucksBadRequestError("Это время было только что занято, попробуйте еще раз.")
            }

            val currentTime = Clock.System.now().toEpochMilliseconds()

            // Не больше полутора часа от текущего времени
            if (request.estimatedTimeToFinish > currentTime + 90 * 60_000) {
                throw DucksBadRequestError("Время заказа должно быть не позже полутора часа.")
            }

            val orderId = CoffeeOrdersTable.insertAndGetId {
                it[createdTime] = currentTime
                it[coffeeShop] = request.shopId
                it[userId] = clientId
                it[comment] = request.comment

                it[estimatedFinishTime] = request.estimatedTimeToFinish
                it[timeToCookInMinutes] = minutesToCookAllProducts

                it[tips] = request.tips
                it[isToTime] = request.isToTime

                // будут посчитаны в конце
                it[price] = 0.toBigDecimal()
                it[totalPrice] = 0.toBigDecimal()
            }

            val allProductIds = request.products.map { it.productId }

            val emptyOrderedProducts = CoffeeProductTable
                .select(
                    CoffeeProductTable.id,
                    CoffeeProductTable.name,
                    CoffeeProductTable.imageUrl,
                    CoffeeProductTable.sizes,
                    CoffeeProductTable.minutesToCook,
                )
                .where {
                    CoffeeProductTable.id inList allProductIds
                }
                .map {
                    OrderedProduct(
                        id = it[CoffeeProductTable.id].value,
                        name = it[CoffeeProductTable.name],
                        imageUrl = it[CoffeeProductTable.imageUrl],
                        minutesToCook = it[CoffeeProductTable.minutesToCook],

                        // Будут заполнены дальше
                        constructors = emptyList(),
                        size = null,
                        quantity = 0,
                        price = null,
                    )
                }

            val orderedProducts = request.products.map { requestProduct ->
                val size = getSelectedSize(
                    sizeId = requestProduct.sizeId,
                    productId = requestProduct.productId
                )

                val constructors = getSelectedConstructors(
                    productId = requestProduct.productId,
                    requestedConstructorIds = requestProduct.constructorIds ?: emptyList(),
                )

                val price = calculateProductPrice(
                    size = size,
                    constructors = constructors,
                    quantity = requestProduct.quantity ?: 1,
                )

                val orderedProduct = emptyOrderedProducts.first { it.id == requestProduct.productId }
                    .copy(
                        constructors = constructors,
                        size = size,
                        quantity = requestProduct.quantity ?: 1,
                        price = price,
                    )

                orderedProduct
            }

            val orderPrice = orderedProducts.sumOf { it.price ?: 0.toBigDecimal() }

            CoffeeOrderedProductsTable.batchInsert(orderedProducts) { product ->
                this[CoffeeOrderedProductsTable.orderId] = orderId
                this[CoffeeOrderedProductsTable.productName] = product.name
                this[CoffeeOrderedProductsTable.productId] = product.id
                this[CoffeeOrderedProductsTable.imageUrl] = product.imageUrl
                this[CoffeeOrderedProductsTable.selectedSize] = product.size ?: throw IllegalStateException("size is required")
                this[CoffeeOrderedProductsTable.constructors] = product.constructors
                this[CoffeeOrderedProductsTable.minutesToCook] = product.minutesToCook?.let {
                    it * product.quantity
                }
                this[CoffeeOrderedProductsTable.quantity] = product.quantity
                this[CoffeeOrderedProductsTable.price] = product.price
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[price] = orderPrice
                it[totalPrice] = orderPrice + (request.tips ?: 0.toBigDecimal())
            }

            getShopFcmToken(request.shopId)
        }

        calculateCoffeeShopsOrdersTimeService(request.shopId)

        sellerFcmToken?.let {
            pushNotificationService.sendToSeller(
                fcmToken = it,
                title = "Новый заказ",
                body = "У вас новый заказ, посмотрите детали.",
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

    private fun getSelectedSize(
        sizeId: String,
        productId: Long
    ): CoffeeProductSizeDTO? {
        return CoffeeProductTable
            .select(CoffeeProductTable.sizes)
            .where {
                CoffeeProductTable.id eq productId
            }.map {
                it[CoffeeProductTable.sizes].firstOrNull { it.id == sizeId }
            }.firstOrNull()
    }

    private fun getSelectedConstructors(
        requestedConstructorIds: List<Long>,
        productId: Long,
    ): List<OrderedProductConstructorDBModel> {
        val constructors = CoffeeProductsWithConstructorsTable
            .join(
                otherTable = CoffeeConstructorsTable,
                joinType = JoinType.LEFT,
                onColumn = CoffeeProductsWithConstructorsTable.constructor,
                otherColumn = CoffeeConstructorsTable.id,
            )
            .select(
                CoffeeConstructorsTable.id,
                CoffeeConstructorsTable.name,
                CoffeeConstructorsTable.price,
            )
            .where {
                (CoffeeProductsWithConstructorsTable.product eq productId) and
                        (CoffeeProductsWithConstructorsTable.constructor inList requestedConstructorIds)
            }.map {
                OrderedProductConstructorDBModel(
                    it[CoffeeConstructorsTable.id].value,
                    it[CoffeeConstructorsTable.name],
                    it[CoffeeConstructorsTable.price],
                )
            }

        return constructors
    }

    private fun userHasActiveOrder(userId: Long): Boolean {
        return CoffeeOrdersTable
            .selectAll()
            .where {
                CoffeeOrdersTable.userId eq userId
            }
            .map {
                it[CoffeeOrdersTable.finishedTime] == null
            }.any { it }
    }

    private fun getUserIdByPhone(clientPhoneNumber: String): Long {
        val clientId = UserTable
            .select(UserTable.id)
            .where {
                UserTable.phoneNumber eq clientPhoneNumber
            }
            .map {
                it[UserTable.id].value
            }
            .first()

        return clientId
    }

    private fun calculateProductPrice(
        size: CoffeeProductSizeDTO?,
        constructors: List<OrderedProductConstructorDBModel>,
        quantity: Int,
    ): BigDecimal {
        val sizePrice = size?.price ?: 0.toBigDecimal()
        val constructorsPrice = constructors.sumOf { it.price ?: 0.toBigDecimal() }

        val singleProductPrice = sizePrice + constructorsPrice

        return singleProductPrice.times(quantity.toBigDecimal())
    }

    private fun calculateOrderFinishTimeMs(
        shopId: Long,
        productIds: List<Long>,
    ): Pair<Long?, Int> {
        // Делаем так потому что обычным select + where можно не получить два продукта с одинаковым id.
        val minutesToCook = productIds.mapNotNull {
            CoffeeProductTable
                .select(CoffeeProductTable.minutesToCook)
                .where {
                    CoffeeProductTable.id eq it
                }.firstNotNullOfOrNull {
                    it[CoffeeProductTable.minutesToCook]
                }
        }.takeIf {
            it.isNotEmpty()
        }?.sumOf {
            it
        } ?: 0

        val closestTimeToStartMs = CoffeeShopTable
            .select(CoffeeShopTable.closestTimeToTakeOrders)
            .where {
                CoffeeShopTable.id eq shopId
            }
            .map {
                it[CoffeeShopTable.closestTimeToTakeOrders]
            }
            .first()

        val timeToFinishMs = closestTimeToStartMs?.let {
            it + minutesToCook.times(60_000)
        }

        return timeToFinishMs to minutesToCook
    }

    private data class OrderedProduct(
        val id: Long,
        val name: String,
        val size: CoffeeProductSizeDTO?,
        val imageUrl: String,
        val constructors: List<OrderedProductConstructorDBModel>,
        val minutesToCook: Int?,
        val quantity: Int,
        val price: BigDecimal?,
    )
}
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
import com.ducks.util.DucksBadRequestError
import io.ktor.server.application.*
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

    suspend fun createOrder(
        request: CreateOrderRequest,
        clientPhoneNumber: String,
    ) {
        newSuspendedTransaction {
            val clientId = getUserIdByPhone(clientPhoneNumber)

            if (userHasActiveOrder(clientId)) {
                throw DucksBadRequestError("Вы не можете создать новый заказ, когда у вас есть активный заказ")
            }

            val (estimatedTimeToFinish, secondsToCookAllProducts) = calculateOrderFinishTime(
                request.shopId,
                request.products.map { it.productId },
            )

            if (estimatedTimeToFinish == null) {
                throw DucksBadRequestError("У кофешопа нет свободного времени для принятия заказа.")
            }

            // Если посчитанное время окончания заказа отличается от времени выбранного клиентом более чем на 2 минуты, выбрасываем ошибку.
            if (estimatedTimeToFinish > request.estimatedTimeToFinish + 2.times(60_000)) {
                throw DucksBadRequestError("Это время было только что занято, попробуйте еще раз.")
            }

            val currentTime = System.currentTimeMillis()

            val orderId = CoffeeOrdersTable.insertAndGetId {
                it[createdTime] = currentTime
                it[coffeeShop] = request.shopId
                it[userId] = clientId
                it[comment] = request.comment

                it[estimatedFinishTime] = estimatedTimeToFinish
                it[timeToCookInMinutes] = secondsToCookAllProducts / 60

                // будет посчитана в конце
                it[price] = 0.toBigDecimal()
            }

            val allProductIds = request.products.map { it.productId }

            val emptyOrderedProducts = CoffeeProductTable
                .select(
                    CoffeeProductTable.id,
                    CoffeeProductTable.name,
                    CoffeeProductTable.imageUrl,
                    CoffeeProductTable.sizes,
                    CoffeeProductTable.secondsToCook,
                )
                .where {
                    CoffeeProductTable.id inList allProductIds
                }
                .map {
                    OrderedProduct(
                        id = it[CoffeeProductTable.id].value,
                        name = it[CoffeeProductTable.name],
                        imageUrl = it[CoffeeProductTable.imageUrl],
                        secondsToCook = it[CoffeeProductTable.secondsToCook],

                        // Будут заполнены дальше
                        constructors = emptyList(),
                        size = null,
                    )
                }

            val orderedProducts = request.products.map { requestProduct ->
                val size = getSelectedSize(
                    sizeName = requestProduct.sizeName,
                    productId = requestProduct.productId
                )

                val constructors = getSelectedConstructors(
                    productId = requestProduct.productId,
                    requestedConstructorIds = requestProduct.constructorIds ?: emptyList(),
                )

                val orderedProduct = emptyOrderedProducts.first { it.id == requestProduct.productId }
                    .copy(
                        constructors = constructors,
                        size = size
                    )

                orderedProduct
            }

            val orderPrice = calculateOrderPrice(orderedProducts)

            CoffeeOrderedProductsTable.batchInsert(orderedProducts) {
                this[CoffeeOrderedProductsTable.orderId] = orderId
                this[CoffeeOrderedProductsTable.productName] = it.name
                this[CoffeeOrderedProductsTable.productId] = it.id
                this[CoffeeOrderedProductsTable.imageUrl] = it.imageUrl
                this[CoffeeOrderedProductsTable.selectedSize] = it.size?.sizeName
                this[CoffeeOrderedProductsTable.constructors] = it.constructors
                this[CoffeeOrderedProductsTable.secondsToCook] = it.secondsToCook
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[price] = orderPrice
            }
        }

        calculateCoffeeShopsOrdersTimeService(request.shopId)
    }

    private fun getSelectedSize(
        sizeName: String,
        productId: Long
    ): CoffeeProductSizeDTO? {
        return CoffeeProductTable
            .select(CoffeeProductTable.sizes)
            .where {
                CoffeeProductTable.id eq productId
            }.map {
                it[CoffeeProductTable.sizes].first { it.sizeName == sizeName }
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

    private fun calculateOrderPrice(products: List<OrderedProduct>): BigDecimal {
        return products.sumOf {
            val productPrice = it.size?.price ?: 0.toBigDecimal()
            val constructorsPrice = it.constructors.sumOf { it.price ?: 0.toBigDecimal() }

            productPrice + constructorsPrice
        }
    }

    private fun calculateOrderFinishTime(
        shopId: Long,
        productIds: List<Long>,
    ): Pair<Long?, Int> {
        // Делаем так потому что обычным select + where можно не получить два продукта с одинаковым id.
        val secondsToCook = productIds.map {
            CoffeeProductTable
                .select(CoffeeProductTable.secondsToCook)
                .where {
                    CoffeeProductTable.id eq it
                }
                .map {
                    it[CoffeeProductTable.secondsToCook]
                }
                .first()
        }.sumOf { it }

        val closestTimeToStart = CoffeeShopTable
            .select(CoffeeShopTable.closestTimeToTakeOrders)
            .where {
                CoffeeShopTable.id eq shopId
            }
            .map {
                it[CoffeeShopTable.closestTimeToTakeOrders]
            }
            .first()

        val secondsToCookAllProducts = secondsToCook.times(1000)

        val timeToFinish = closestTimeToStart?.let {
            it + secondsToCookAllProducts
        }

        return timeToFinish to secondsToCookAllProducts
    }

    private data class OrderedProduct(
        val id: Long,
        val name: String,
        val size: CoffeeProductSizeDTO?,
        val imageUrl: String,
        val constructors: List<OrderedProductConstructorDBModel>,
        val secondsToCook: Int,
    )
}
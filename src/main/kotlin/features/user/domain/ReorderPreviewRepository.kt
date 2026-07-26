package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeProductsWithConstructorsTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.user.data.dto.ReorderPreviewDTO
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal

class ReorderPreviewRepository(
    private val calculator: ReorderPreviewCalculator = ReorderPreviewCalculator(),
) {

    suspend fun getReorderPreview(orderId: Long, userId: Long): ReorderPreviewDTO? {
        return newSuspendedTransaction {
            val orderExists = CoffeeOrdersTable
                .select(CoffeeOrdersTable.id)
                .where { (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.userId eq userId) }
                .firstOrNull() != null

            if (!orderExists) return@newSuspendedTransaction null

            val orderedProducts = CoffeeOrderedProductsTable
                .select(
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
                    ReorderPreviewCalculator.OrderedProduct(
                        productId = it[CoffeeOrderedProductsTable.productId],
                        name = it[CoffeeOrderedProductsTable.productName],
                        imageUrl = it[CoffeeOrderedProductsTable.imageUrl],
                        size = it[CoffeeOrderedProductsTable.selectedSize],
                        constructors = it[CoffeeOrderedProductsTable.constructors] ?: emptyList(),
                        quantity = it[CoffeeOrderedProductsTable.quantity],
                        price = it[CoffeeOrderedProductsTable.price] ?: BigDecimal.ZERO,
                    )
                }

            val currentProducts = orderedProducts
                .groupBy { it.productId }
                .mapValues { (productId, productLines) ->
                    val orderedConstructorIds = productLines
                        .flatMap { line -> line.constructors.map { it.id } }
                        .distinct()
                    fetchCurrentProduct(productId, orderedConstructorIds)
                }

            calculator.calculate(orderedProducts, currentProducts)
        }
    }

    private fun fetchCurrentProduct(
        productId: Long,
        orderedConstructorIds: List<Long>,
    ): ReorderPreviewCalculator.CurrentProduct? {
        val row = CoffeeProductTable
            .select(
                CoffeeProductTable.name,
                CoffeeProductTable.imageUrl,
                CoffeeProductTable.sizes,
                CoffeeProductTable.inStock,
            )
            .where { CoffeeProductTable.id eq productId }
            .firstOrNull()
            ?: return null

        return ReorderPreviewCalculator.CurrentProduct(
            name = row[CoffeeProductTable.name],
            imageUrl = row[CoffeeProductTable.imageUrl],
            inStock = row[CoffeeProductTable.inStock],
            sizes = row[CoffeeProductTable.sizes],
            constructors = currentConstructors(productId, orderedConstructorIds),
        )
    }

    private fun currentConstructors(
        productId: Long,
        constructorIds: List<Long>,
    ): Map<Long, ReorderPreviewCalculator.CurrentConstructor> {
        if (constructorIds.isEmpty()) return emptyMap()

        return CoffeeProductsWithConstructorsTable
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
                CoffeeConstructorsTable.isInStock,
            )
            .where {
                (CoffeeProductsWithConstructorsTable.product eq productId) and
                        (CoffeeProductsWithConstructorsTable.constructor inList constructorIds)
            }
            .associate {
                it[CoffeeConstructorsTable.id].value to ReorderPreviewCalculator.CurrentConstructor(
                    name = it[CoffeeConstructorsTable.name],
                    price = it[CoffeeConstructorsTable.price],
                    inStock = it[CoffeeConstructorsTable.isInStock],
                )
            }
    }
}

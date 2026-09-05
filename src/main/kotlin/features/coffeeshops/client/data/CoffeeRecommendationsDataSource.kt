package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.CartProduct
import com.ducks.features.coffeeshops.client.data.model.ProductGroup
import com.ducks.features.coffeeshops.client.data.model.ProductPairScore
import com.ducks.features.coffeeshops.client.data.model.RecommendationCandidate
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.database.mappers.mapToProductPreviewDTO
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class CoffeeRecommendationsDataSource {

    fun fetchCartProducts(shopId: Long, productIds: List<Long>): List<CartProduct> {
        if (productIds.isEmpty()) return emptyList()

        return CoffeeProductTable
            .join(CoffeeProductCategoryTable, JoinType.INNER, CoffeeProductTable.categoryId, CoffeeProductCategoryTable.id)
            .join(CoffeeCategoryGroupTable, JoinType.INNER, CoffeeProductCategoryTable.groupId, CoffeeCategoryGroupTable.id)
            .select(
                CoffeeProductTable.id,
                CoffeeProductTable.minutesToCook,
                CoffeeProductTable.cooksInParallel,
                CoffeeCategoryGroupTable.name,
            )
            .where {
                (CoffeeProductTable.shopId eq shopId) and (CoffeeProductTable.id inList productIds.distinct())
            }
            .map { row ->
                CartProduct(
                    id = row[CoffeeProductTable.id].value,
                    group = ProductGroup.ofName(row[CoffeeCategoryGroupTable.name]),
                    minutesToCook = row[CoffeeProductTable.minutesToCook] ?: 0,
                    cooksInParallel = row[CoffeeProductTable.cooksInParallel],
                )
            }
    }

    fun fetchCandidates(shopId: Long, excludedProductIds: Set<Long>): List<RecommendationCandidate> {
        return CoffeeProductTable
            .join(CoffeeProductCategoryTable, JoinType.INNER, CoffeeProductTable.categoryId, CoffeeProductCategoryTable.id)
            .join(CoffeeCategoryGroupTable, JoinType.INNER, CoffeeProductCategoryTable.groupId, CoffeeCategoryGroupTable.id)
            .join(CoffeeShopTable, JoinType.INNER, CoffeeProductTable.shopId, CoffeeShopTable.id)
            .join(CoffeeProductsWithConstructorsTable, JoinType.LEFT, CoffeeProductsWithConstructorsTable.product, CoffeeProductTable.id)
            .join(CoffeeConstructorsTable, JoinType.LEFT, CoffeeConstructorsTable.id, CoffeeProductsWithConstructorsTable.constructor)
            .join(CoffeeModifiedConstructorCategoryTable, JoinType.LEFT, CoffeeProductsWithConstructorsTable.modifiedCategory, CoffeeModifiedConstructorCategoryTable.id)
            .join(CoffeeConstructorCategoryTable, JoinType.LEFT, CoffeeModifiedConstructorCategoryTable.categoryId, CoffeeConstructorCategoryTable.id)
            .selectAll()
            .where {
                (CoffeeProductTable.shopId eq shopId) and (CoffeeProductTable.inStock eq true)
            }
            .toList()
            .filterNot { it[CoffeeProductTable.id].value in excludedProductIds }
            .groupBy { it[CoffeeProductTable.id].value }
            .map { (_, productRows) ->
                RecommendationCandidate(
                    product = productRows.mapToProductPreviewDTO(),
                    group = ProductGroup.ofName(productRows.first()[CoffeeCategoryGroupTable.name]),
                    cooksInParallel = productRows.first()[CoffeeProductTable.cooksInParallel],
                )
            }
            .filter { it.product.sizes.isNotEmpty() }
    }

    /** Сколько штук каждого товара кофейня продала начиная с [from]. */
    fun fetchSoldQuantities(shopId: Long, from: Long): Map<Long, Int> {
        val soldQuantity = CoffeeOrderedProductsTable.quantity.sum()

        return CoffeeOrderedProductsTable
            .join(CoffeeOrdersTable, JoinType.INNER, CoffeeOrderedProductsTable.orderId, CoffeeOrdersTable.id)
            .select(CoffeeOrderedProductsTable.productId, soldQuantity)
            .where { (CoffeeOrdersTable.coffeeShop eq shopId) and isCountableOrder(from) }
            .groupBy(CoffeeOrderedProductsTable.productId)
            .associate { it[CoffeeOrderedProductsTable.productId] to (it[soldQuantity] ?: 0) }
    }

    /**
     * Лучший скор для каждого товара, который берут вместе хоть с чем-то из корзины.
     * Пустая мапа — статистики по этой кофейне ещё нет, подбор уедет на правила.
     */
    fun fetchCoOccurrenceScores(shopId: Long, sourceProductIds: List<Long>): Map<Long, Double> {
        if (sourceProductIds.isEmpty()) return emptyMap()

        return CoffeeProductRecommendationTable
            .select(CoffeeProductRecommendationTable.targetProductId, CoffeeProductRecommendationTable.score)
            .where {
                (CoffeeProductRecommendationTable.shopId eq shopId) and
                        (CoffeeProductRecommendationTable.sourceProductId inList sourceProductIds.distinct())
            }
            .groupingBy { it[CoffeeProductRecommendationTable.targetProductId].value }
            .fold(0.0) { best, row -> maxOf(best, row[CoffeeProductRecommendationTable.score]) }
    }

    /** Кофейни, у которых сейчас лежит посчитанная статистика. */
    fun fetchShopIdsWithRecommendations(): List<Long> {
        return CoffeeProductRecommendationTable
            .select(CoffeeProductRecommendationTable.shopId)
            .groupBy(CoffeeProductRecommendationTable.shopId)
            .map { it[CoffeeProductRecommendationTable.shopId].value }
    }

    /** Кофейни, в которых с [from] был хоть один заказ: пересчитывать остальные незачем. */
    fun fetchShopIdsWithOrders(from: Long): List<Long> {
        return CoffeeOrdersTable
            .select(CoffeeOrdersTable.coffeeShop)
            .where { isCountableOrder(from) }
            .groupBy(CoffeeOrdersTable.coffeeShop)
            .map { it[CoffeeOrdersTable.coffeeShop].value }
    }

    /** Состав заказов кофейни с [from]: id заказа — набор товаров в нём. */
    fun fetchOrdersContent(shopId: Long, from: Long): Map<Long, Set<Long>> {
        return CoffeeOrderedProductsTable
            .join(CoffeeOrdersTable, JoinType.INNER, CoffeeOrderedProductsTable.orderId, CoffeeOrdersTable.id)
            .select(CoffeeOrderedProductsTable.orderId, CoffeeOrderedProductsTable.productId)
            .where { (CoffeeOrdersTable.coffeeShop eq shopId) and isCountableOrder(from) }
            .groupBy(
                keySelector = { it[CoffeeOrderedProductsTable.orderId].value },
                valueTransform = { it[CoffeeOrderedProductsTable.productId] },
            )
            .mapValues { (_, productIds) -> productIds.toSet() }
    }

    /** Товары, которые сейчас в меню кофейни: снятые с продажи в статистике не нужны. */
    fun fetchShopProductIds(shopId: Long): Set<Long> {
        return CoffeeProductTable
            .select(CoffeeProductTable.id)
            .where { CoffeeProductTable.shopId eq shopId }
            .mapTo(mutableSetOf()) { it[CoffeeProductTable.id].value }
    }

    /**
     * Заменяет статистику кофейни целиком. Пустой [pairs] просто вычищает старые пары —
     * так подбор возвращается на правила, когда заказов в окне стало слишком мало.
     */
    fun replaceRecommendations(shopId: Long, pairs: List<ProductPairScore>, calculatedAt: Long) {
        CoffeeProductRecommendationTable.deleteWhere { CoffeeProductRecommendationTable.shopId eq shopId }

        if (pairs.isEmpty()) return

        CoffeeProductRecommendationTable.batchInsert(pairs) { pair ->
            this[CoffeeProductRecommendationTable.shopId] = shopId
            this[CoffeeProductRecommendationTable.sourceProductId] = pair.sourceProductId
            this[CoffeeProductRecommendationTable.targetProductId] = pair.targetProductId
            this[CoffeeProductRecommendationTable.score] = pair.score
            this[CoffeeProductRecommendationTable.pairCount] = pair.pairCount
            this[CoffeeProductRecommendationTable.calculatedAt] = calculatedAt
        }
    }

    /** Заказ, по которому можно судить о спросе: не отменённый и не протухший. */
    private fun SqlExpressionBuilder.isCountableOrder(from: Long): Op<Boolean> =
        (CoffeeOrdersTable.createdTime greaterEq from) and
                (CoffeeOrdersTable.isCancelledBySeller eq false) and
                (CoffeeOrdersTable.isCancelledByClient eq false) and
                (CoffeeOrdersTable.isExpired eq false)
}

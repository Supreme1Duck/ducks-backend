package com.ducks.features.coffeeshops.service

import com.ducks.features.coffeeshops.client.data.model.ProductPairScore

/**
 * Считает по составам заказов, какие товары берут вместе чаще, чем это объясняется
 * их собственной популярностью. Чистая арифметика без базы — вся возня с БД
 * остаётся в [ProductRecommendationsService].
 */
class CoOccurrenceScoreCalculator(
    /** Меньше скольки заказов на пару — это шум, а не закономерность. */
    private val minPairCount: Int = 3,
    /** Насколько товар должен обгонять свою обычную частоту, чтобы попасть в выдачу. */
    private val minLift: Double = 1.0,
    private val maxTargetsPerSource: Int = 10,
) {

    /**
     * @param baskets составы заказов: один элемент — набор товаров одного заказа.
     * Заказы из одного товара обязательно передавать тоже: пар они не дают, но участвуют
     * в знаменателе — без них любой ходовой товар выглядел бы «сочетающимся со всем подряд».
     */
    fun calculate(baskets: List<Set<Long>>): List<ProductPairScore> {
        if (baskets.isEmpty()) return emptyList()

        val ordersWithProduct = mutableMapOf<Long, Int>()
        val pairCounts = mutableMapOf<Pair<Long, Long>, Int>()

        baskets.forEach { basket ->
            basket.forEach { productId ->
                ordersWithProduct[productId] = (ordersWithProduct[productId] ?: 0) + 1
            }

            basket.forEach { source ->
                basket.forEach { target ->
                    if (source != target) {
                        val pair = source to target
                        pairCounts[pair] = (pairCounts[pair] ?: 0) + 1
                    }
                }
            }
        }

        val totalOrders = baskets.size.toDouble()

        return pairCounts
            .mapNotNull { (pair, pairCount) ->
                if (pairCount < minPairCount) return@mapNotNull null

                val (source, target) = pair
                val sourceOrders = ordersWithProduct[source] ?: return@mapNotNull null
                val targetOrders = ordersWithProduct[target] ?: return@mapNotNull null

                // Lift: во сколько раз target чаще встречается в заказах с source, чем вообще.
                // Делить на популярность обязательно — иначе наверх всплывёт самый ходовой
                // эспрессо, который просто есть в половине заказов и сочетается со всем.
                val confidence = pairCount.toDouble() / sourceOrders
                val baseRate = targetOrders / totalOrders
                val lift = confidence / baseRate

                if (lift <= minLift) return@mapNotNull null

                ProductPairScore(
                    sourceProductId = source,
                    targetProductId = target,
                    score = lift,
                    pairCount = pairCount,
                )
            }
            .groupBy { it.sourceProductId }
            .flatMap { (_, targets) ->
                targets.sortedByDescending { it.score }.take(maxTargetsPerSource)
            }
    }
}

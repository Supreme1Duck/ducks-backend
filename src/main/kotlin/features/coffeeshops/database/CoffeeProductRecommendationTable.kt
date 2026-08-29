package com.ducks.features.coffeeshops.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Кэш статистики совместных покупок: как часто [targetProductId] попадает в тот же заказ,
 * что и [sourceProductId]. Заполняется ночным пересчётом по истории заказов и целиком
 * перезаписывается для кофейни — восстановить содержимое можно в любой момент.
 */
object CoffeeProductRecommendationTable : LongIdTable("ducks_coffee_product_recommendation_table") {

    val shopId = reference("shop_id", CoffeeShopTable, onDelete = ReferenceOption.CASCADE)

    val sourceProductId = reference("source_product_id", CoffeeProductTable, onDelete = ReferenceOption.CASCADE)
    val targetProductId = reference("target_product_id", CoffeeProductTable, onDelete = ReferenceOption.CASCADE)

    /** Во сколько раз товар чаще встречается в заказах с source, чем в заказах вообще (lift). */
    val score = double("score")

    /** Сколько заказов дало эту пару. */
    val pairCount = integer("pair_count")

    val calculatedAt = long("calculated_at")

    init {
        uniqueIndex(shopId, sourceProductId, targetProductId)
    }
}

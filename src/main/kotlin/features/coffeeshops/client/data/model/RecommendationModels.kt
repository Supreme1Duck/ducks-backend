package com.ducks.features.coffeeshops.client.data.model

import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import java.math.BigDecimal

/**
 * Группа категорий в терминах рекомендаций.
 * Группы заведены миграцией V201 и заводятся дальше только руками через админку,
 * поэтому опознаём их по имени; всё незнакомое считаем «Дополнительно» —
 * такая группа безопасна: её можно предлагать к любой корзине.
 */
enum class ProductGroup {
    DRINKS,
    FOOD,
    EXTRA;

    companion object {
        fun ofName(name: String): ProductGroup = when (name.trim().lowercase()) {
            "напитки" -> DRINKS
            "еда" -> FOOD
            else -> EXTRA
        }
    }
}

/** Товар, уже лежащий в корзине: всё, что нужно для подбора рекомендаций. */
data class CartProduct(
    val id: Long,
    val group: ProductGroup,
    val minutesToCook: Int,
    val cooksInParallel: Boolean,
)

/** Кандидат в рекомендации — готовый превью-DTO плюс группа его категории. */
data class RecommendationCandidate(
    val product: CoffeeShopProductPreviewDTO,
    val group: ProductGroup,
    val cooksInParallel: Boolean,
) {
    val minutesToCook: Int get() = product.minutesToCook ?: 0

    val minPrice: BigDecimal get() = product.sizes.minOfOrNull { it.price } ?: BigDecimal.ZERO
}

/** Строка статистики совместных покупок, посчитанная ночным пересчётом. */
data class ProductPairScore(
    val sourceProductId: Long,
    val targetProductId: Long,
    val score: Double,
    val pairCount: Int,
)

package com.ducks.features.coffeeshops.client.domain

import com.ducks.features.coffeeshops.client.data.CoffeeRecommendationsDataSource
import com.ducks.features.coffeeshops.client.data.model.ProductGroup
import com.ducks.features.coffeeshops.client.data.model.RecommendationCandidate
import com.ducks.features.coffeeshops.client.data.model.dto.CartRecommendationDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CartRecommendationsResponse
import com.ducks.features.coffeeshops.client.routings.request.CartRecommendationsRequest
import com.ducks.features.coffeeshops.service.CookingTimeCalculator
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Подбор допродажи к корзине.
 *
 * Если у кофейни набралась статистика совместных покупок (её ночью считает
 * [com.ducks.features.coffeeshops.service.ProductRecommendationsService]), берём её:
 * реальные заказы знают о сочетаниях больше любых правил.
 *
 * Пока статистики нет — правила: рекомендуем не похожее, а дополняющее. Второй капучино
 * к капучино никто не купит, поэтому к напиткам предлагаем еду, к еде — напиток,
 * а к полной корзине — только «Дополнительно».
 */
class CartRecommendationsRepository(
    private val dataSource: CoffeeRecommendationsDataSource,
    private val cookingTimeCalculator: CookingTimeCalculator = CookingTimeCalculator(),
) {

    suspend fun recommendForCart(request: CartRecommendationsRequest): CartRecommendationsResponse {
        val limit = (request.limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)

        return newSuspendedTransaction {
            val cart = dataSource.fetchCartProducts(request.shopId, request.productIds)
            if (cart.isEmpty()) return@newSuspendedTransaction EMPTY_RESPONSE

            val cartGroups = cart.map { it.group }.toSet()
            val quantityById = request.productIds.groupingBy { it }.eachCount()

            // Считаем ровно тем же калькулятором, что и /products/estimate-cooking-time:
            // «+N мин» на карточке обязан совпасть со временем, которое клиент увидит
            // следующим запросом, уже добавив товар в корзину.
            val cartCookingItems = cart.map { product ->
                CookingTimeCalculator.CookingItem(
                    minutesToCook = product.minutesToCook,
                    cooksInParallel = product.cooksInParallel,
                    quantity = quantityById[product.id] ?: 1,
                )
            }

            val candidates = dataSource
                .fetchCandidates(request.shopId, excludedProductIds = cart.map { it.id }.toSet())

            val soldQuantities = dataSource.fetchSoldQuantities(
                shopId = request.shopId,
                from = System.currentTimeMillis() - POPULARITY_PERIOD_MILLIS,
            )

            // Статистика заказов кофейни: чем чаще товар берут вместе с содержимым корзины,
            // тем выше он в выдаче. Групповые правила к таким товарам не применяем — если
            // люди действительно берут два кофе в один заказ, мешать им незачем.
            //
            // Слой необязательный: нет таблицы (код уехал вперёд миграции) — деградируем до
            // правил, а не отдаём 500. Поэтому запрос обязан оставаться последним обращением
            // к базе в транзакции: упавший statement аварит её целиком.
            val coOccurrenceScores = try {
                dataSource.fetchCoOccurrenceScores(
                    shopId = request.shopId,
                    sourceProductIds = cart.map { it.id },
                )
            } catch (e: Exception) {
                println("Статистика совместных покупок недоступна, подбор идёт по правилам: $e")
                emptyMap()
            }

            val byStatistics = candidates
                .filter { coOccurrenceScores.containsKey(it.product.id) }
                .sortedWith(
                    compareBy<RecommendationCandidate> { extraMinutes(it, cartCookingItems) > 0 }
                        .thenByDescending { coOccurrenceScores[it.product.id] ?: 0.0 }
                )

            val targetGroups = targetGroups(cartGroups)
            // Если в подходящих группах ничего нет — лучше показать хоть что-то из меню,
            // чем пустую карусель: у маленьких кофеен вся еда может быть распродана.
            val byRules = candidates
                .filter { it.group in targetGroups }
                .ifEmpty { candidates }
                .filterNot { coOccurrenceScores.containsKey(it.product.id) }
                .sortedWith(
                    compareBy<RecommendationCandidate> { extraMinutes(it, cartCookingItems) > 0 }
                        .thenByDescending { soldQuantities[it.product.id] ?: 0 }
                        .thenBy { it.minPrice }
                )

            CartRecommendationsResponse(
                title = if (byStatistics.isEmpty()) title(cartGroups) else STATISTICS_TITLE,
                products = pickDiverse(byStatistics + byRules, limit).map { candidate ->
                    CartRecommendationDTO(
                        product = candidate.product,
                        extraMinutes = extraMinutes(candidate, cartCookingItems),
                        isQuickAdd = candidate.isQuickAdd(),
                    )
                },
            )
        }
    }

    private fun targetGroups(cartGroups: Set<ProductGroup>): Set<ProductGroup> = when {
        cartGroups == setOf(ProductGroup.DRINKS) -> setOf(ProductGroup.FOOD, ProductGroup.EXTRA)
        cartGroups == setOf(ProductGroup.FOOD) -> setOf(ProductGroup.DRINKS, ProductGroup.EXTRA)
        else -> setOf(ProductGroup.EXTRA)
    }

    private fun title(cartGroups: Set<ProductGroup>): String = when {
        cartGroups == setOf(ProductGroup.DRINKS) -> "Что-нибудь к напитку?"
        cartGroups == setOf(ProductGroup.FOOD) -> "Добавить напиток?"
        else -> "Добавить к заказу?"
    }

    /**
     * На сколько минут товар сдвинет готовность заказа. Ноль — не сдвинет вовсе:
     * так будет у сэндвича, который греется в печи короче, чем бариста собирает
     * остальную корзину, и у всего, что стоит готовым в витрине.
     */
    private fun extraMinutes(
        candidate: RecommendationCandidate,
        cartCookingItems: List<CookingTimeCalculator.CookingItem>,
    ): Int = cookingTimeCalculator.extraMinutes(
        items = cartCookingItems,
        addition = CookingTimeCalculator.CookingItem(
            minutesToCook = candidate.minutesToCook,
            cooksInParallel = candidate.cooksInParallel,
        ),
    )

    /**
     * Шесть сэндвичей подряд — это не рекомендации, а вторая витрина, поэтому из одной
     * категории берём не больше [MAX_PER_CATEGORY] позиций. Если так набралось меньше
     * запрошенного, добираем остатком в том же порядке ранжирования.
     */
    private fun pickDiverse(ranked: List<RecommendationCandidate>, limit: Int): List<RecommendationCandidate> {
        val takenPerCategory = mutableMapOf<Long, Int>()
        val picked = mutableListOf<RecommendationCandidate>()

        for (candidate in ranked) {
            if (picked.size == limit) break

            val categoryId = candidate.product.categoryId
            val taken = takenPerCategory[categoryId] ?: 0
            if (taken >= MAX_PER_CATEGORY) continue

            takenPerCategory[categoryId] = taken + 1
            picked += candidate
        }

        if (picked.size < limit) {
            picked += ranked.filterNot { it in picked }.take(limit - picked.size)
        }

        return picked
    }

    /**
     * В один тап добавляется только то, что нельзя собрать неправильно:
     * один размер и ни одной категории конструкторов с обязательным выбором.
     */
    private fun RecommendationCandidate.isQuickAdd(): Boolean =
        product.sizes.size <= 1 && product.constructors.none { (it.category.minSelection ?: 0) > 0 }

    private companion object {
        const val DEFAULT_LIMIT = 6
        const val MAX_LIMIT = 12
        const val MAX_PER_CATEGORY = 2

        const val POPULARITY_PERIOD_MILLIS = 30L * 24 * 60 * 60 * 1000

        const val STATISTICS_TITLE = "Часто берут вместе"

        val EMPTY_RESPONSE = CartRecommendationsResponse(title = "", products = emptyList())
    }
}

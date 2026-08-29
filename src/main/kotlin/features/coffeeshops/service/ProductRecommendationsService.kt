package com.ducks.features.coffeeshops.service

import com.ducks.features.coffeeshops.client.data.CoffeeRecommendationsDataSource
import com.ducks.util.APP_ZONE_OFFSET
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration.Companion.days

/**
 * Ночной пересчёт статистики совместных покупок: по истории заказов каждой кофейни
 * считает, какие товары попадают в один заказ чаще, чем попадали бы по случайности,
 * и складывает результат в [com.ducks.features.coffeeshops.database.CoffeeProductRecommendationTable].
 *
 * Считается именно так, а не на лету: подбор рекомендаций дёргается на каждое открытие
 * корзины, а история заказов за квартал меняется медленно — за сутки ничего не протухнет.
 */
class ProductRecommendationsService(
    private val dataSource: CoffeeRecommendationsDataSource,
    private val calculator: CoOccurrenceScoreCalculator = CoOccurrenceScoreCalculator(),
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Ночной и ручной пересчёты не должны идти одновременно: каждый переписывает
    // статистику кофейни целиком, и вдвоём они просто дважды сделают одну работу.
    private val mutex = Mutex()

    fun invoke() {
        scope.launch {
            while (true) {
                delay(millisUntilNextRun())
                recalculateAll()
            }
        }
    }

    /**
     * Ручной пересчёт из админки: та же работа, что ночью, но по требованию и с отчётом.
     * [shopId] = null — все кофейни, у которых есть заказы в окне истории.
     *
     * Считает синхронно, в запросе: пересчёт идёт по десяткам кофеен и укладывается
     * в секунды, а админу нужен результат, а не «задача принята».
     */
    suspend fun recalculateNow(shopId: Long? = null): RecalculationReport = mutex.withLock {
        val from = System.currentTimeMillis() - HISTORY_PERIOD.inWholeMilliseconds

        if (shopId == null) return recalculate(from)

        // Одна кофейня — ошибку не глотаем: админ должен увидеть, что пересчёт не удался.
        val pairs = newSuspendedTransaction { recalculateShop(shopId, from) }

        return RecalculationReport(shops = 1, pairs = pairs, failed = 0)
    }

    private suspend fun recalculateAll() = mutex.withLock {
        recalculate(from = System.currentTimeMillis() - HISTORY_PERIOD.inWholeMilliseconds)
    }

    private suspend fun recalculate(from: Long): RecalculationReport {
        val shopIds = newSuspendedTransaction { dataSource.fetchShopIdsWithOrders(from) }

        // Кофейня, у которой за всё окно не осталось ни одного заказа, статистики иметь
        // не должна: иначе закрывшаяся точка вечно рекомендовала бы прошлогоднее меню.
        newSuspendedTransaction {
            dataSource.fetchShopIdsWithRecommendations()
                .filterNot { it in shopIds }
                .forEach { dataSource.replaceRecommendations(it, pairs = emptyList(), calculatedAt = System.currentTimeMillis()) }
        }

        var pairs = 0
        var failed = 0

        // Каждая кофейня — отдельная транзакция: пересчёт одной не должен ронять остальные.
        shopIds.forEach { shopId ->
            try {
                pairs += newSuspendedTransaction { recalculateShop(shopId, from) }
            } catch (e: Exception) {
                failed++
                println("Пересчёт рекомендаций для кофейни $shopId не удался: $e")
            }
        }

        return RecalculationReport(shops = shopIds.size - failed, pairs = pairs, failed = failed)
    }

    /** @return сколько пар записано кофейне. */
    private fun recalculateShop(shopId: Long, from: Long): Int {
        val orders = dataSource.fetchOrdersContent(shopId, from)

        // На маленькой выборке пары — это шум: две случайные покупки подряд дают «закономерность».
        // Пока заказов мало, статистику держим пустой и подбор идёт по правилам.
        if (orders.size < MIN_SHOP_ORDERS) {
            dataSource.replaceRecommendations(shopId, pairs = emptyList(), calculatedAt = System.currentTimeMillis())
            return 0
        }

        // Снятые с продажи товары из статистики выкидываем: рекомендовать их всё равно нечем.
        val menuProductIds = dataSource.fetchShopProductIds(shopId)
        val baskets = orders.values.map { it intersect menuProductIds }

        val pairs = calculator.calculate(baskets)

        dataSource.replaceRecommendations(shopId, pairs, calculatedAt = System.currentTimeMillis())

        return pairs.size
    }

    /** Ближайшие [RUN_AT] по времени приложения (UTC+3) — самый тихий час у кофеен. */
    private fun millisUntilNextRun(): Long {
        val now = LocalDateTime.now(APP_ZONE_OFFSET)
        val today = LocalDate.now(APP_ZONE_OFFSET).atTime(RUN_AT)
        val next = if (now < today) today else today.plusDays(1)

        return next.toInstant(APP_ZONE_OFFSET).toEpochMilli() - now.toInstant(APP_ZONE_OFFSET).toEpochMilli()
    }

    @Serializable
    data class RecalculationReport(
        // Сколько кофеен пересчитано. Кофейня без заказов в окне сюда не попадает:
        // пересчитывать в ней нечего, её статистика просто чистится.
        val shops: Int,
        val pairs: Int,
        val failed: Int,
    )

    private companion object {
        val RUN_AT: LocalTime = LocalTime.of(4, 0)

        /** Окно истории: короче — статистики не наберётся, длиннее — тянем прошлогоднее меню. */
        val HISTORY_PERIOD = 90.days

        const val MIN_SHOP_ORDERS = 200
    }
}

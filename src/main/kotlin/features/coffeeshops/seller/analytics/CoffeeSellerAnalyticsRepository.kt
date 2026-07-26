package com.ducks.features.coffeeshops.seller.analytics

import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.util.APP_ZONE_OFFSET
import features.coffeeshops.seller.analytics.CoffeeShopAnalytics
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

class CoffeeSellerAnalyticsRepository {

    suspend fun getAnalytics(coffeeShopId: Long): CoffeeShopAnalytics = newSuspendedTransaction {
        val now = System.currentTimeMillis()

        // Дни и месяцы считаются в UTC+3 — так же, как в списке заказов за день.
        val today = LocalDate.now(APP_ZONE_OFFSET)

        val todayStart = today.atStartOfDay().toInstant(APP_ZONE_OFFSET).toEpochMilli()
        val yesterdayStart = todayStart - 86_400_000L

        val monthStart = today.withDayOfMonth(1).atStartOfDay().toInstant(APP_ZONE_OFFSET).toEpochMilli()
        val lastMonthStart =
            today.minusMonths(1).withDayOfMonth(1).atStartOfDay().toInstant(APP_ZONE_OFFSET).toEpochMilli()
        val lastMonthEnd = monthStart

        fun completedOrders() = CoffeeOrdersTable
            .select(CoffeeOrdersTable.price, CoffeeOrdersTable.createdTime)
            .where {
                (CoffeeOrdersTable.coffeeShop eq coffeeShopId) and
                        (CoffeeOrdersTable.isCancelledBySeller eq false) and
                        (CoffeeOrdersTable.isCancelledByClient eq false) and
                        (CoffeeOrdersTable.isExpired eq false)
            }

        fun revenueInRange(from: Long, to: Long): Double =
            completedOrders()
                .andWhere { (CoffeeOrdersTable.createdTime greaterEq from) and (CoffeeOrdersTable.createdTime less to) }
                .sumOf { it[CoffeeOrdersTable.price].toDouble() }

        fun ordersCountInRange(from: Long, to: Long): Int =
            completedOrders()
                .andWhere { (CoffeeOrdersTable.createdTime greaterEq from) and (CoffeeOrdersTable.createdTime less to) }
                .count().toInt()

        fun percentChange(current: Double, previous: Double): Double {
            if (previous == 0.0) {
                return if (current == 0.0) {
                    0.0
                } else {
                    100.0
                }
            }

            return ((current - previous) / previous) * 100.0
        }

        val todayRevenue = revenueInRange(todayStart, now)
        val yesterdayRevenue = revenueInRange(yesterdayStart, todayStart)

        val todayOrders = ordersCountInRange(todayStart, now)
        val yesterdayOrders = ordersCountInRange(yesterdayStart, todayStart)

        val thisMonthRevenue = revenueInRange(monthStart, now)
        val lastMonthRevenue = revenueInRange(lastMonthStart, lastMonthEnd)

        val topProducts = fetchTopProducts(
            shopId = coffeeShopId,
            startTimeToCountFrom = monthStart,
        )

        fun cancelledBySellerInRange(from: Long, to: Long): Int =
            CoffeeOrdersTable
                .select(CoffeeOrdersTable.id)
                .where {
                    (CoffeeOrdersTable.coffeeShop eq coffeeShopId) and
                            (CoffeeOrdersTable.isCancelledBySeller eq true) and
                            (CoffeeOrdersTable.createdTime greaterEq from) and
                            (CoffeeOrdersTable.createdTime less to)
                }
                .count().toInt()

        fun cancelledByClientInRange(from: Long, to: Long): Int =
            CoffeeOrdersTable
                .select(CoffeeOrdersTable.id)
                .where {
                    (CoffeeOrdersTable.coffeeShop eq coffeeShopId) and
                            (CoffeeOrdersTable.isCancelledByClient eq true) and
                            (CoffeeOrdersTable.createdTime greaterEq from) and
                            (CoffeeOrdersTable.createdTime less to)
                }
                .count().toInt()

        fun expiredInRange(from: Long, to: Long): Int =
            CoffeeOrdersTable
                .select(CoffeeOrdersTable.id)
                .where {
                    (CoffeeOrdersTable.coffeeShop eq coffeeShopId) and
                            (CoffeeOrdersTable.isExpired eq true) and
                            (CoffeeOrdersTable.createdTime greaterEq from) and
                            (CoffeeOrdersTable.createdTime less to)
                }
                .count().toInt()

        val thisMonthCancelledBySeller = cancelledBySellerInRange(monthStart, now)
        val thisMonthCancelledByClient = cancelledByClientInRange(monthStart, now)
        val thisMonthExpired = expiredInRange(monthStart, now)

        CoffeeShopAnalytics(
            dailyRevenue = CoffeeShopAnalytics.PeriodStats(
                value = todayRevenue,
                percentChange = percentChange(todayRevenue, yesterdayRevenue)
            ),
            dailyOrdersCount = CoffeeShopAnalytics.PeriodStats(
                value = todayOrders.toDouble(),
                percentChange = percentChange(todayOrders.toDouble(), yesterdayOrders.toDouble())
            ),
            monthlyRevenue = CoffeeShopAnalytics.PeriodStats(
                value = thisMonthRevenue,
                percentChange = percentChange(thisMonthRevenue, lastMonthRevenue)
            ),
            topProducts = topProducts,
            monthlyCancelledBySeller = thisMonthCancelledBySeller,
            monthlyCancelledByClient = thisMonthCancelledByClient,
            monthlyExpired = thisMonthExpired,
            allDaysMonthsRevenue = getDailyRevenueForCurrentMonth(coffeeShopId)
        )
    }

    // С начала месяца 5 лучших продуктов
    private fun fetchTopProducts(
        shopId: Long,
        startTimeToCountFrom: Long,
    ): List<CoffeeShopAnalytics.TopProduct> {
        return CoffeeOrderedProductsTable
            .join(CoffeeOrdersTable, JoinType.INNER, CoffeeOrderedProductsTable.orderId, CoffeeOrdersTable.id)
            .select(
                CoffeeOrderedProductsTable.productId,
                CoffeeOrderedProductsTable.productName,
                CoffeeOrderedProductsTable.quantity.sum(),
                CoffeeOrderedProductsTable.price.sum(),
            )
            .where {
                (CoffeeOrdersTable.coffeeShop eq shopId) and
                        (CoffeeOrdersTable.createdTime greaterEq startTimeToCountFrom) and
                        (CoffeeOrdersTable.isCancelledBySeller eq false) and
                        (CoffeeOrdersTable.isCancelledByClient eq false) and
                        (CoffeeOrdersTable.isExpired eq false)
            }
            .groupBy(
                CoffeeOrderedProductsTable.productId,
                CoffeeOrderedProductsTable.productName,
            )
            .orderBy(CoffeeOrderedProductsTable.quantity.sum(), SortOrder.DESC)
            .limit(5)
            .map {
                CoffeeShopAnalytics.TopProduct(
                    productId = it[CoffeeOrderedProductsTable.productId],
                    productName = it[CoffeeOrderedProductsTable.productName],
                    ordersCount = it[CoffeeOrderedProductsTable.quantity.sum()] ?: 0,
                    totalRevenue = it[CoffeeOrderedProductsTable.price.sum()]?.toDouble() ?: 0.0
                )
            }
    }

    private suspend fun getDailyRevenueForCurrentMonth(coffeeShopId: Long): List<CoffeeShopAnalytics.DayRevenue> =
        newSuspendedTransaction {
            val today = LocalDate.now(APP_ZONE_OFFSET)
            val currentDay = today.dayOfMonth

            (1..currentDay).map { day ->
                val dayStart = today.withDayOfMonth(day)
                    .atStartOfDay()
                    .toInstant(APP_ZONE_OFFSET)
                    .toEpochMilli()
                val dayEnd = dayStart + 86_400_000L

                val revenue = CoffeeOrdersTable
                    .select(CoffeeOrdersTable.price)
                    .where {
                        (CoffeeOrdersTable.coffeeShop eq coffeeShopId) and
                                (CoffeeOrdersTable.isCancelledBySeller eq false) and
                                (CoffeeOrdersTable.isCancelledByClient eq false) and
                                (CoffeeOrdersTable.isExpired eq false) and
                                (CoffeeOrdersTable.createdTime greaterEq dayStart) and
                                (CoffeeOrdersTable.createdTime less dayEnd)
                    }
                    .sumOf { it[CoffeeOrdersTable.price].toDouble() }

                CoffeeShopAnalytics.DayRevenue(
                    dayInMonth = day,
                    revenue = revenue,
                )
            }
        }
}
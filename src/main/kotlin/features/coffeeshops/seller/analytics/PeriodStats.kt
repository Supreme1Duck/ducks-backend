package features.coffeeshops.seller.analytics

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeShopAnalytics(
    val dailyRevenue: PeriodStats,
    val dailyOrdersCount: PeriodStats,
    val monthlyRevenue: PeriodStats,
    val topProducts: List<TopProduct>,
    val monthlyCancelledBySeller: Int,
    val monthlyCancelledByClient: Int,
    val monthlyExpired: Int,
    val allDaysMonthsRevenue: List<DayRevenue>,
) {
    @Serializable
    data class PeriodStats(
        val value: Double,
        val percentChange: Double
    )

    @Serializable
    data class TopProduct(
        val productId: Long,
        val productName: String,
        val ordersCount: Int,
        val totalRevenue: Double
    )

    @Serializable
    data class DayRevenue(
        val dayInMonth: Int,
        val revenue: Double,
    )
}
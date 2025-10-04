package features.orders.data.model

// Описывает слот времени занятого кофешопом (заказ, тех пауза).
data class BusyTimeSlotsData(
    val startTime: Long,
    val endTime: Long,
)
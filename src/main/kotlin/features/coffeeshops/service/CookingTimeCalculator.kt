package com.ducks.features.coffeeshops.service

/**
 * Время приготовления заказа.
 *
 * Позиции, которые бариста собирает руками, складываются: пока он льёт латте, второй
 * латте не готовится. Позиции с [CookingItem.cooksInParallel] — сэндвич в печи, готовый
 * десерт из витрины — идут одновременно с остальной работой, поэтому в сумму не входят
 * и сдвигают выдачу, только если греются дольше всего остального заказа.
 */
class CookingTimeCalculator {

    data class CookingItem(
        val minutesToCook: Int,
        val cooksInParallel: Boolean,
        val quantity: Int = 1,
    )

    fun minutesToCook(items: List<CookingItem>): Int {
        val (parallel, serial) = items.partition { it.cooksInParallel }

        val handsBusyMinutes = serial.sumOf { it.minutesToCook * it.quantity }
        // Партия греется вместе, поэтому количество здесь не умножается: два сэндвича
        // уезжают в печь одной закладкой и готовы через то же время, что и один.
        val longestParallelMinutes = parallel.maxOfOrNull { it.minutesToCook } ?: 0

        return maxOf(handsBusyMinutes, longestParallelMinutes) + HANDOVER_BUFFER_MINUTES
    }

    /** На сколько минут [addition] сдвинет готовность заказа [items]. */
    fun extraMinutes(items: List<CookingItem>, addition: CookingItem): Int =
        minutesToCook(items + addition) - minutesToCook(items)

    private companion object {
        /** Минута на выдачу: ровно столько же добавляла прежняя оценка, суммировавшая всё подряд. */
        const val HANDOVER_BUFFER_MINUTES = 1
    }
}

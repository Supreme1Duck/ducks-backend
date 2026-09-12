package com.ducks.features.coffeeshops.service

/**
 * Время приготовления заказа.
 *
 * Позиции, которые бариста собирает руками, складываются: пока он льёт латте, второй
 * латте не готовится. Позиции с [CookingItem.cooksInParallel] — сэндвич в печи, готовый
 * десерт из витрины — идут одновременно с остальной работой, поэтому в сумму не входят
 * и сдвигают выдачу, только если греются дольше всего остального заказа.
 *
 * В режиме [CookingMode.SPLIT_ORDER] руки считаются на двоих: заказ раскладывается между
 * двумя бариста. Ускоряется при этом заказ, а не отдельный товар — один раф на семь минут
 * останется рафом на семь минут.
 */
class CookingTimeCalculator {

    data class CookingItem(
        val minutesToCook: Int,
        val cooksInParallel: Boolean,
        val quantity: Int = 1,
    )

    fun minutesToCook(items: List<CookingItem>, mode: CookingMode = CookingMode.NORMAL): Int {
        val (parallel, serial) = items.partition { it.cooksInParallel }

        val handsBusyMinutes = when (mode) {
            CookingMode.NORMAL -> serial.sumOf { it.minutesToCook * it.quantity }
            CookingMode.SPLIT_ORDER -> minutesOnTwoBaristas(serial)
        }
        // Партия греется вместе, поэтому количество здесь не умножается: два сэндвича
        // уезжают в печь одной закладкой и готовы через то же время, что и один.
        // Второй бариста печь не ускоряет, так что режим на эту часть не влияет.
        val longestParallelMinutes = parallel.maxOfOrNull { it.minutesToCook } ?: 0

        return maxOf(handsBusyMinutes, longestParallelMinutes) + HANDOVER_BUFFER_MINUTES
    }

    /** На сколько минут [addition] сдвинет готовность заказа [items]. */
    fun extraMinutes(
        items: List<CookingItem>,
        addition: CookingItem,
        mode: CookingMode = CookingMode.NORMAL,
    ): Int = minutesToCook(items + addition, mode) - minutesToCook(items, mode)

    /**
     * Сколько заняты руки, когда заказ разбирают двое.
     *
     * Делится заказ по единицам товара: два латте бариста разберут по одному, а один
     * латте пополам не делится — поэтому позиция целиком уходит кому-то одному и задаёт
     * нижнюю границу времени.
     *
     * Раскладываем жадно, от самых долгих позиций к коротким, каждую — тому, кто
     * освободится раньше. Формула «сумма пополам» здесь врёт вниз: три напитка по три
     * минуты не разложить на две смены по четыре с половиной, кому-то достанется шесть.
     */
    private fun minutesOnTwoBaristas(serial: List<CookingItem>): Int {
        val units = serial
            .flatMap { item -> List(item.quantity) { item.minutesToCook } }
            .sortedDescending()

        var firstBarista = 0
        var secondBarista = 0

        units.forEach { minutes ->
            if (firstBarista <= secondBarista) {
                firstBarista += minutes
            } else {
                secondBarista += minutes
            }
        }

        return maxOf(firstBarista, secondBarista)
    }

    private companion object {
        /** Минута на выдачу: ровно столько же добавляла прежняя оценка, суммировавшая всё подряд. */
        const val HANDOVER_BUFFER_MINUTES = 1
    }
}

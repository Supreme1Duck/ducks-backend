package features.coffeeshops.service

import com.ducks.features.coffeeshops.service.CoOccurrenceScoreCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoOccurrenceScoreCalculatorTest {

    private val calculator = CoOccurrenceScoreCalculator()

    private fun basket(vararg productIds: Long) = productIds.toSet()

    private fun List<com.ducks.features.coffeeshops.client.data.model.ProductPairScore>.scoreOf(
        source: Long,
        target: Long,
    ) = firstOrNull { it.sourceProductId == source && it.targetProductId == target }?.score

    @Test
    fun `пара из одного-двух заказов не попадает в статистику`() {
        val baskets = List(50) { basket(LATTE) } + List(2) { basket(LATTE, CROISSANT) }

        assertTrue(calculator.calculate(baskets).isEmpty())
    }

    @Test
    fun `устойчивое сочетание попадает в статистику в обе стороны`() {
        val baskets = List(40) { basket(LATTE) } +
                List(10) { basket(LATTE, CROISSANT) } +
                List(40) { basket(TEA) }

        val pairs = calculator.calculate(baskets)

        assertTrue((pairs.scoreOf(LATTE, CROISSANT) ?: 0.0) > 1.0)
        assertTrue((pairs.scoreOf(CROISSANT, LATTE) ?: 0.0) > 1.0)
        assertNull(pairs.scoreOf(TEA, CROISSANT))
    }

    @Test
    fun `ходовой товар не вытесняет реально сочетающийся`() {
        // Эспрессо есть почти в каждом заказе, а сироп берут только к латте.
        val baskets = List(60) { basket(LATTE, ESPRESSO, SYRUP) } +
                List(60) { basket(TEA, ESPRESSO) } +
                List(60) { basket(ESPRESSO) }

        val pairs = calculator.calculate(baskets)

        val syrupScore = pairs.scoreOf(LATTE, SYRUP)
        val espressoScore = pairs.scoreOf(LATTE, ESPRESSO)

        assertTrue(syrupScore != null && syrupScore > 1.0, "сироп должен попасть в пары к латте")
        // Эспрессо встречается со всеми одинаково часто, так что рекомендацией он не является.
        assertNull(espressoScore)
    }

    @Test
    fun `на один товар отдаётся не больше maxTargetsPerSource пар`() {
        val calculator = CoOccurrenceScoreCalculator(maxTargetsPerSource = 3)

        // Плюс заказы без латте: иначе латте есть в каждом заказе и о сочетаниях
        // не говорит ничего — lift выходит ровно 1 и пары честно отсеиваются.
        val baskets = (1L..10L).flatMap { target -> List(5) { basket(LATTE, target * 100) } } +
                List(50) { basket(TEA) }

        val pairs = calculator.calculate(baskets).filter { it.sourceProductId == LATTE }

        assertEquals(3, pairs.size)
    }

    @Test
    fun `пустая история не ломает расчёт`() {
        assertTrue(calculator.calculate(emptyList()).isEmpty())
        assertTrue(calculator.calculate(listOf(emptySet())).isEmpty())
    }

    private companion object {
        const val LATTE = 1L
        const val CROISSANT = 2L
        const val TEA = 3L
        const val ESPRESSO = 4L
        const val SYRUP = 5L
    }
}

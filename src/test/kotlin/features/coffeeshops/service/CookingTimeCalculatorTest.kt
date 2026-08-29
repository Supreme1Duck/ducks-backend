package features.coffeeshops.service

import com.ducks.features.coffeeshops.service.CookingTimeCalculator
import com.ducks.features.coffeeshops.service.CookingTimeCalculator.CookingItem
import kotlin.test.Test
import kotlin.test.assertEquals

class CookingTimeCalculatorTest {

    private val calculator = CookingTimeCalculator()

    private fun serial(minutes: Int, quantity: Int = 1) =
        CookingItem(minutesToCook = minutes, cooksInParallel = false, quantity = quantity)

    private fun parallel(minutes: Int, quantity: Int = 1) =
        CookingItem(minutesToCook = minutes, cooksInParallel = true, quantity = quantity)

    @Test
    fun `товары без флага считаются суммой, как раньше`() {
        assertEquals(4, calculator.minutesToCook(listOf(serial(3))))
        assertEquals(9, calculator.minutesToCook(listOf(serial(3), serial(5))))
        assertEquals(7, calculator.minutesToCook(listOf(serial(3, quantity = 2))))
    }

    @Test
    fun `пустой заказ — минута на выдачу`() {
        assertEquals(1, calculator.minutesToCook(emptyList()))
    }

    @Test
    fun `параллельный товар прячется за временем остального заказа`() {
        // Латте собирается 6 минут, сэндвич греется 4 — он успевает внутри этого времени.
        assertEquals(7, calculator.minutesToCook(listOf(serial(6), parallel(4))))
    }

    @Test
    fun `параллельный товар дольше остального заказа задаёт время сам`() {
        assertEquals(9, calculator.minutesToCook(listOf(serial(3), parallel(8))))
    }

    @Test
    fun `партия параллельных товаров греется вместе`() {
        // Два сэндвича уезжают в печь одной закладкой, время от этого не удваивается.
        assertEquals(6, calculator.minutesToCook(listOf(parallel(5, quantity = 2))))
        assertEquals(6, calculator.minutesToCook(listOf(parallel(5), parallel(4))))
    }

    @Test
    fun `сдвиг выдачи считается от реального времени заказа`() {
        val cart = listOf(serial(6))

        // Сэндвич успевает внутри уже занятых шести минут — заказ не сдвигается.
        assertEquals(0, calculator.extraMinutes(cart, parallel(4)))
        // Тот же сэндвич, но собираемый руками, добавляется к очереди целиком.
        assertEquals(4, calculator.extraMinutes(cart, serial(4)))
        // Печь дольше всей остальной работы — сдвиг ровно на разницу.
        assertEquals(2, calculator.extraMinutes(cart, parallel(8)))
    }
}

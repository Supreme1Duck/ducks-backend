package features.coffeeshops.service

import com.ducks.features.coffeeshops.service.CookingMode
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

    @Test
    fun `x2 — один товар вдвоём быстрее не сделать`() {
        // Раф на семь минут остаётся рафом на семь минут, сколько бы бариста ни стояло.
        assertEquals(8, calculator.minutesToCook(listOf(serial(7)), CookingMode.SPLIT_ORDER))
        assertEquals(8, calculator.minutesToCook(listOf(serial(7))))
    }

    @Test
    fun `x2 — заказ разбирают двое`() {
        // Латте и капучино уходят разным бариста: заказ идёт по самой долгой позиции.
        assertEquals(4, calculator.minutesToCook(listOf(serial(3), serial(2)), CookingMode.SPLIT_ORDER))
        // Два одинаковых напитка — по одному на каждого, количество время не удваивает.
        assertEquals(4, calculator.minutesToCook(listOf(serial(3, quantity = 2)), CookingMode.SPLIT_ORDER))
    }

    @Test
    fun `x2 — раскладка честнее, чем сумма пополам`() {
        // Три напитка по три минуты: пополам вышло бы 4.5, но третий напиток целиком
        // достаётся кому-то одному — 3 + 3 против 3.
        assertEquals(7, calculator.minutesToCook(listOf(serial(3, quantity = 3)), CookingMode.SPLIT_ORDER))
        // Долгая позиция уходит первой, две короткие складываются у второго бариста.
        assertEquals(6, calculator.minutesToCook(listOf(serial(5), serial(2), serial(3)), CookingMode.SPLIT_ORDER))
    }

    @Test
    fun `x2 — печь не ускоряется вторым баристой`() {
        // Руки освобождаются за 3 минуты вместо 6, но сэндвич всё те же 8 минут в печи.
        assertEquals(
            9,
            calculator.minutesToCook(listOf(serial(3), serial(3), parallel(8)), CookingMode.SPLIT_ORDER),
        )
    }

    @Test
    fun `x2 — пустой заказ и один товар считаются как обычно`() {
        assertEquals(1, calculator.minutesToCook(emptyList(), CookingMode.SPLIT_ORDER))
        assertEquals(4, calculator.minutesToCook(listOf(serial(3)), CookingMode.SPLIT_ORDER))
    }

    @Test
    fun `x2 — сдвиг выдачи считается в том же режиме`() {
        // Второй бариста свободен, пока первый льёт латте: капучино проезжает бесплатно.
        assertEquals(0, calculator.extraMinutes(listOf(serial(5)), serial(4), CookingMode.SPLIT_ORDER))
        // Оба заняты — добавка ложится к тому, кто освободится раньше.
        assertEquals(2, calculator.extraMinutes(listOf(serial(5), serial(4)), serial(3), CookingMode.SPLIT_ORDER))
    }
}

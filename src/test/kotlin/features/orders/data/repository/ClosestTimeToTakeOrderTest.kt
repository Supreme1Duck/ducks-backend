package features.orders.data.repository

import com.ducks.features.orders.data.model.IsCoffeeShopReadyToOrderReason
import com.ducks.features.orders.data.model.WorkTimeModel
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import features.orders.data.model.BusyTimeSlotsData
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClosestTimeToTakeOrderTest {

    private val repo = FetchAvailableOrdersTimeListRepository()

    // +03:00 — московское время (как в продакшне)
    private val tz = ZoneOffset.ofHours(3)

    // Все времена в тестах — понедельник 2025-01-06
    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2025, 1, 6, hour, minute).toEpochSecond(tz) * 1000

    private val now = at(18, 0)

    // Кофешоп работает 08:00–23:00
    private val workTime = WorkTimeModel(
        dayOfWeek = 1,
        startTime = at(8, 0),
        endTime = at(23, 0),
        isClosed = false,
    )

    private fun slot(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
    ) = BusyTimeSlotsData(startTime = at(startHour, startMinute), endTime = at(endHour, endMinute))

    private fun calculate(
        slots: List<BusyTimeSlotsData>,
        shopWorkTime: WorkTimeModel? = workTime,
    ) = repo.calculateClosestTimeToTakeOrder(
        // getAllBusyTimeSlots отдаёт слоты отсортированными по endTime — воспроизводим это здесь
        busyTimeSlotsDataList = slots.sortedBy { it.endTime },
        coffeeShopWorkTime = shopWorkTime,
        currentTime = now,
    )

    @Test
    fun `окно после первого заказа — берётся 18h05, а не конец последнего заказа`() {
        // сейчас 18:00; заказы заканчиваются в 18:05 и 18:50
        val result = calculate(
            listOf(
                slot(17, 55, 18, 5),
                slot(18, 40, 18, 50),
            )
        )

        assertEquals(at(18, 5), result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ReadyToTake, result.reason)
    }

    @Test
    fun `три заказа — берётся первое подходящее окно, а не последнее`() {
        val result = calculate(
            listOf(
                slot(17, 50, 18, 5),
                slot(18, 30, 18, 45),
                slot(18, 47, 18, 55),
            )
        )

        assertEquals(at(18, 5), result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ReadyToTake, result.reason)
    }

    @Test
    fun `окно между 5 и 10 минутами — время то же, но только короткий заказ`() {
        // окно 18:05 → 18:12 = 7 минут
        val result = calculate(
            listOf(
                slot(17, 55, 18, 5),
                slot(18, 12, 18, 50),
            )
        )

        assertEquals(at(18, 5), result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.OnlyShortOrder, result.reason)
    }

    @Test
    fun `окна между заказами нет — берётся конец последнего заказа`() {
        // окно 18:05 → 18:08 = 3 минуты, меньше 5 — окном не считается
        val result = calculate(
            listOf(
                slot(17, 55, 18, 5),
                slot(18, 8, 18, 50),
            )
        )

        assertEquals(at(18, 50), result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ReadyToTake, result.reason)
    }

    @Test
    fun `нет занятых слотов — можно принять заказ прямо сейчас`() {
        val result = calculate(emptyList())

        assertEquals(now, result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ReadyToTake, result.reason)
    }

    @Test
    fun `первый заказ начнётся через 20 минут — можно принять заказ прямо сейчас`() {
        val result = calculate(listOf(slot(18, 20, 18, 40)))

        assertEquals(now, result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ReadyToTake, result.reason)
    }

    @Test
    fun `первый заказ начнётся через 7 минут — сейчас, но только короткий заказ`() {
        val result = calculate(listOf(slot(18, 7, 18, 40)))

        assertEquals(now, result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.OnlyShortOrder, result.reason)
    }

    @Test
    fun `ближайшее время дальше часа от текущего — слишком много заказов`() {
        val result = calculate(listOf(slot(18, 0, 19, 10)))

        assertNull(result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.TooManyOrders, result.reason)
    }

    @Test
    fun `нет расписания — кофешоп закрыт`() {
        val result = calculate(listOf(slot(17, 55, 18, 5)), shopWorkTime = null)

        assertNull(result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ShopClosed, result.reason)
    }

    @Test
    fun `ближайшее время выходит за конец смены — кофешоп закрыт`() {
        // смена заканчивается в 18:30, а свободное время только в 18:50
        val shortShift = workTime.copy(endTime = at(18, 30))
        val result = calculate(
            listOf(
                slot(17, 55, 18, 5),
                slot(18, 8, 18, 50),
            ),
            shopWorkTime = shortShift,
        )

        assertNull(result.closestTime)
        assertEquals(IsCoffeeShopReadyToOrderReason.ShopClosed, result.reason)
    }
}

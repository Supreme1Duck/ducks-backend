package features.orders.data.repository

import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkTimeCalculationTest {

    private val repo = FetchAvailableOrdersTimeListRepository()

    // +03:00 — московское время (как в продакшне)
    private val tz = ZoneOffset.ofHours(3)

    // Вспомогательный метод: создаёт OffsetDateTime для заданного дня недели/времени.
    // dayOfWeek: 1=пн, 7=вс (ISO)
    // Базовая точка отсчёта — понедельник 2025-01-06T00:00+03:00
    private fun at(dayOfWeek: Int, hour: Int, minute: Int = 0): OffsetDateTime {
        val monday = LocalDateTime.of(2025, 1, 6, 0, 0) // понедельник
        return monday.plusDays((dayOfWeek - 1).toLong())
            .plusHours(hour.toLong())
            .plusMinutes(minute.toLong())
            .atOffset(tz)
    }

    private fun scheduleFor(
        dayOfWeek: Int,
        startTime: String?,
        endTime: String?,
        isClosed: Boolean = false,
    ) = FetchAvailableOrdersTimeListRepository.Schedule(
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        isClosed = isClosed,
    )

    // --- Базовые кейсы ---

    @Test
    fun `возвращает null если расписание пустое`() {
        val result = repo.calculateWorkTime(emptyList(), at(3, 14))
        assertNull(result)
    }

    @Test
    fun `возвращает null если сегодня выходной`() {
        val schedule = listOf(scheduleFor(3, "10:00", "22:00", isClosed = true))
        val result = repo.calculateWorkTime(schedule, at(3, 14))
        assertNull(result)
    }

    @Test
    fun `кофешоп открыт — текущее время внутри смены`() {
        // среда 10:00–22:00, сейчас среда 14:00
        val schedule = listOf(scheduleFor(3, "10:00", "22:00"))
        val result = repo.calculateWorkTime(schedule, at(3, 14))
        assertNotNull(result)
        assertTrue(!result.isClosed)
    }

    @Test
    fun `кофешоп ещё не открылся сегодня — возвращает смену сегодня`() {
        // среда 13:00–22:00, сейчас среда 10:00
        val schedule = listOf(scheduleFor(3, "13:00", "22:00"))
        val result = repo.calculateWorkTime(schedule, at(3, 10))
        assertNotNull(result)
        // openTime должно быть среда 13:00
        val expectedStart = at(3, 13).toEpochSecond() * 1000
        assertTrue(result.startTime == expectedStart, "startTime=${result.startTime}, expected=$expectedStart")
    }

    // --- Ночная смена (через полночь) ---

    @Test
    fun `ночная смена — 01h00 четверга попадает в среду 13h00–03h00`() {
        // среда работает с 13:00 до 03:00 (следующего дня)
        // сейчас 01:00 четверга
        val schedule = (1..7).map { day ->
            if (day == 3) scheduleFor(3, "13:00", "03:00")  // среда
            else scheduleFor(day, null, null, isClosed = true)
        }
        val now = at(4, 1)  // четверг 01:00
        val result = repo.calculateWorkTime(schedule, now)
        assertNotNull(result, "Должна вернуться ночная смена среды")
        assertTrue(!result.isClosed)
        // endTime должно быть четверг 03:00
        val expectedEnd = at(4, 3).toEpochSecond() * 1000
        assertTrue(result.endTime == expectedEnd, "endTime=${result.endTime}, expected=$expectedEnd")
    }

    @Test
    fun `ночная смена — 03h01 четверга уже после смены среды`() {
        // сейчас 03:01 четверга, смена среды закончилась в 03:00
        val schedule = (1..7).map { day ->
            if (day == 3) scheduleFor(3, "13:00", "03:00")
            else scheduleFor(day, null, null, isClosed = true)
        }
        val now = at(4, 3, 1)  // четверг 03:01
        val result = repo.calculateWorkTime(schedule, now)
        assertNull(result, "После окончания ночной смены должен быть null")
    }

    // --- Выходной сегодня ---

    @Test
    fun `сегодня выходной — возвращает завтрашнюю смену`() {
        // среда — выходной, четверг — рабочий
        val schedule = listOf(
            scheduleFor(3, "10:00", "22:00", isClosed = true),
            scheduleFor(4, "10:00", "22:00"),
        )
        val now = at(3, 14)
        val result = repo.calculateWorkTime(schedule, now)
        assertNotNull(result, "Должна вернуться смена четверга")
        val expectedStart = now.plusDays(1).toLocalDate()
            .toEpochSecond(java.time.LocalTime.of(10, 0), ZoneOffset.ofHours(3)) * 1000
        assertTrue(result.startTime == expectedStart, "startTime должен быть четверг 10:00")
    }

    // --- Граничные значения ---

    @Test
    fun `кофешоп открылся ровно в это время — граница включительно не входит`() {
        // 10:00–22:00, сейчас ровно 10:00 — текущее время == startTime, не внутри смены
        val schedule = listOf(scheduleFor(3, "10:00", "22:00"))
        val now = at(3, 10)
        val result = repo.calculateWorkTime(schedule, now)
        // nowInMs > startTime — строгое неравенство, поэтому 10:00 не попадёт в activeShift
        // но вернётся через fallback (currentDay)
        assertNotNull(result)
        val expectedStart = at(3, 10).toEpochSecond() * 1000
        assertTrue(result.startTime == expectedStart)
    }

    @Test
    fun `startTime и endTime равны — вырожденная смена, возвращает null`() {
        // 00:00–00:00: диапазон пустой, текущее время никогда не попадёт внутрь
        val schedule = listOf(scheduleFor(3, "00:00", "00:00"))
        val result = repo.calculateWorkTime(schedule, at(3, 14))
        assertNull(result)
    }

    // --- Несколько дней ---

    @Test
    fun `несколько рабочих дней — возвращает активный сегодняшний`() {
        val schedule = listOf(
            scheduleFor(2, "09:00", "21:00"),
            scheduleFor(3, "10:00", "22:00"),
            scheduleFor(4, "11:00", "23:00"),
        )
        val result = repo.calculateWorkTime(schedule, at(3, 15))  // среда 15:00
        assertNotNull(result)
        val expectedStart = at(3, 10).toEpochSecond() * 1000
        assertTrue(result.startTime == expectedStart)
    }

    @Test
    fun `isClosed=false но startTime=null — пропускается`() {
        val schedule = listOf(scheduleFor(3, null, null, isClosed = false))
        val result = repo.calculateWorkTime(schedule, at(3, 14))
        assertNull(result)
    }

    // --- Граница недели вс→пн ---

    @Test
    fun `ночная смена воскресенья — 01h00 понедельника попадает в вс 10h00–03h00`() {
        // Воскресенье работает с 10:00 до 03:00 (пн)
        // Сейчас 01:00 понедельника — смена воскресенья ещё идёт
        val schedule = (1..7).map { day ->
            if (day == 7) scheduleFor(7, "10:00", "03:00")  // воскресенье
            else scheduleFor(day, null, null, isClosed = true)
        }
        val now = at(1, 1)  // понедельник 01:00
        val result = repo.calculateWorkTime(schedule, now)
        assertNotNull(result, "Должна вернуться ночная смена воскресенья")
        assertTrue(!result.isClosed)
        // endTime должно быть понедельник 03:00
        val expectedEnd = at(1, 3).toEpochSecond() * 1000
        assertTrue(result.endTime == expectedEnd, "endTime=${result.endTime}, expected=$expectedEnd")
    }

    @Test
    fun `ночная смена воскресенья — 03h01 понедельника уже после конца смены`() {
        val schedule = (1..7).map { day ->
            if (day == 7) scheduleFor(7, "10:00", "03:00")
            else scheduleFor(day, null, null, isClosed = true)
        }
        val now = at(1, 3, 1)  // понедельник 03:01
        val result = repo.calculateWorkTime(schedule, now)
        assertNull(result, "После конца ночной смены воскресенья должен быть null")
    }

    // --- Кофешоп уже закрыт ---

    @Test
    fun `воскресенье 23h00 — смена закончилась в 22h00, завтра нет расписания — null`() {
        val schedule = listOf(scheduleFor(7, "10:00", "22:00"))
        val result = repo.calculateWorkTime(schedule, at(7, 23))
        assertNull(result, "Нет расписания на завтра — возвращаем null")
    }

    @Test
    fun `воскресенье 23h00 — смена закончилась в 22h00, возвращает завтрашнюю смену`() {
        val schedule = listOf(
            scheduleFor(7, "10:00", "22:00"),  // воскресенье — уже закрыто
            scheduleFor(1, "09:00", "21:00"),  // понедельник — завтра
        )
        val now = at(7, 23)
        val result = repo.calculateWorkTime(schedule, now)
        assertNotNull(result, "Должна вернуться завтрашняя смена понедельника")
        // Ожидаем понедельник 09:00 — это now + 1 день, а не at(1,9) (другая неделя)
        val expectedStart = now.plusDays(1).toLocalDate()
            .toEpochSecond(java.time.LocalTime.of(9, 0), ZoneOffset.ofHours(3)) * 1000
        assertTrue(result.startTime == expectedStart, "startTime должен быть следующий понедельник 09:00")
    }

    // --- Смена до полуночи (endTime = "00:00") ---

    @Test
    fun `смена до полуночи — 23h00 воскресенья внутри смены вс 10h00–00h00`() {
        // "00:00" как endTime означает полночь: смена идёт через полночь по логике кода
        val schedule = listOf(scheduleFor(7, "10:00", "00:00"))
        val result = repo.calculateWorkTime(schedule, at(7, 23))
        assertNotNull(result, "23:00 должно попадать в смену до полуночи")
    }

    @Test
    fun `смена до полуночи — ровно 00h00 понедельника включительно входит в диапазон`() {
        // "00:00" как endTime → endDateTime = понедельник 00:00
        // Kotlin range (..) включает правую границу, поэтому 00:00 == endTime входит
        val schedule = listOf(scheduleFor(7, "10:00", "00:00"))
        val now = at(1, 0)  // понедельник 00:00 (= воскресенье "24:00")
        val result = repo.calculateWorkTime(schedule, now)
        assertNotNull(result, "Ровно в 00:00 диапазон startTime..endTime включает правую границу")
    }

    @Test
    fun `смена до полуночи — 00h01 понедельника уже после конца смены`() {
        val schedule = listOf(scheduleFor(7, "10:00", "00:00"))
        val now = at(1, 0, 1)  // понедельник 00:01
        val result = repo.calculateWorkTime(schedule, now)
        assertNull(result, "00:01 уже после конца смены")
    }
}

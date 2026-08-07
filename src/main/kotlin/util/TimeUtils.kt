package com.ducks.util

import java.time.ZoneOffset

// Часовой пояс приложения: UTC+3 (Москва, без DST).
// В нём считаются границы дня для отчётов и списков за день.
val APP_ZONE_OFFSET: ZoneOffset = ZoneOffset.ofHours(3)

private const val MINUTE_IN_MS = 60_000L

/**
 * Округляет время вверх до ближайшей целой минуты (16:27:20 -> 16:28:00).
 * Ровная минута остаётся без изменений. Границы минут одинаковы в любом
 * часовом поясе, поэтому считаем чистой арифметикой без учёта смещения.
 */
fun ceilToMinute(timeMs: Long): Long = ((timeMs + MINUTE_IN_MS - 1) / MINUTE_IN_MS) * MINUTE_IN_MS

/**
 * Округляет время вниз до ближайшей целой минуты (16:27:20 -> 16:27:00).
 * Ровная минута остаётся без изменений.
 */
fun floorToMinute(timeMs: Long): Long = (timeMs / MINUTE_IN_MS) * MINUTE_IN_MS

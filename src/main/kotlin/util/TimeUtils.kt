package com.ducks.util

import java.time.ZoneOffset

// Часовой пояс приложения: UTC+3 (Москва, без DST).
// В нём считаются границы дня для отчётов и списков за день.
val APP_ZONE_OFFSET: ZoneOffset = ZoneOffset.ofHours(3)

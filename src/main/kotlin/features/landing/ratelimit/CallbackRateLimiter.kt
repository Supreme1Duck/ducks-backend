package com.ducks.features.landing.ratelimit

import com.ducks.common.ratelimit.SlidingWindowLimiter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Форма на лендинге открыта наружу и без авторизации, поэтому единственное, что
 * отделяет наш чат от потока мусора, — эти счётчики. Лимит на ip и отдельный на номер:
 * первый закрывает скрипт, который дёргает ручку, второй — повторные нажатия кнопки.
 *
 * Состояние в памяти: заявок мало, а после перезапуска сервера счётчики можно и обнулить —
 * ради этого держать таблицу в базе незачем.
 */
class CallbackRateLimiter {

    private val ipRequests = SlidingWindowLimiter(IP_LIMIT, IP_WINDOW_SECONDS)
    private val phoneRequests = ConcurrentHashMap<String, Instant>()

    companion object {
        private const val IP_LIMIT = 5
        private const val IP_WINDOW_SECONDS = 3600L

        // Один и тот же номер второй раз за час — это либо промах, либо нетерпение:
        // в чат такое дублировать не нужно.
        private const val PHONE_COOLDOWN_SECONDS = 3600L
    }

    /** @return сколько секунд ждать, если с этого ip заявок уже слишком много, иначе null. */
    fun checkIpLimit(ip: String): Long? = ipRequests.check(ip)

    /** @return сколько секунд ждать, если с этого номера только что была заявка, иначе null. */
    fun checkPhoneLimit(phone: String): Long? {
        val last = phoneRequests[phone] ?: return null
        val elapsed = Instant.now().epochSecond - last.epochSecond

        return if (elapsed < PHONE_COOLDOWN_SECONDS) PHONE_COOLDOWN_SECONDS - elapsed else null
    }

    fun recordIpRequest(ip: String) = ipRequests.record(ip)

    fun recordPhoneRequest(phone: String) {
        phoneRequests[phone] = Instant.now()
    }
}

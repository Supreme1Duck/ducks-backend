package com.ducks.features.user.ratelimit

import com.ducks.common.ratelimit.SlidingWindowLimiter

/**
 * Анонимный вход создаёт нового пользователя на каждый вызов и ничем не защищён, так что без
 * лимита на ip скрипт забил бы таблицу пользователей мусором.
 *
 * Лимит с запасом: за одним ip бывает много клиентов сразу (мобильный NAT, wi-fi кофейни),
 * а честный клиент входит один раз на установку приложения.
 *
 * Состояние в памяти, как и у остальных лимитеров: после перезапуска счётчики можно обнулить.
 */
class LoginRateLimiter {

    private val ipRequestsLimiter = SlidingWindowLimiter(IP_LIMIT, IP_WINDOW_SECONDS)

    companion object {
        private const val IP_LIMIT = 20
        private const val IP_WINDOW_SECONDS = 3600L
    }

    /** @return сколько секунд ждать, если с этого ip входов уже слишком много, иначе null. */
    fun checkIpLimit(ip: String): Long? = ipRequestsLimiter.check(ip)

    fun recordIpRequest(ip: String) = ipRequestsLimiter.record(ip)
}

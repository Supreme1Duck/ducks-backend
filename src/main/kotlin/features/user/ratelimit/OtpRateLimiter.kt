package com.ducks.features.user.ratelimit

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class OtpRateLimiter {

    data class PhoneState(
        val callCount: Int,
        val lastCallTime: Instant,
    )

    private val phoneStates = ConcurrentHashMap<String, PhoneState>()
    private val ipRequests = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    companion object {
        // Cooldown in seconds indexed by number of previous calls (capped at last element)
        // [0] = after 1st call (1st repeat) → 60s
        // [1] = after 2nd call (2nd repeat) → 300s
        // [2] = after 3rd call (3rd repeat) → 300s
        // [3] = after 4th+ call              → 1800s
        private val PHONE_COOLDOWNS = longArrayOf(60L, 300L, 300L, 1800L)
        private const val PHONE_RESET_SECONDS = 1800L

        private const val IP_LIMIT = 10
        private const val IP_WINDOW_SECONDS = 3600L
    }

    /**
     * Returns seconds to wait if the phone number is rate-limited, null if allowed.
     */
    fun checkPhoneLimit(phoneNumber: String): Long? {
        val state = phoneStates[phoneNumber] ?: return null
        val elapsed = Instant.now().epochSecond - state.lastCallTime.epochSecond
        if (elapsed >= PHONE_RESET_SECONDS) return null
        val cooldown = PHONE_COOLDOWNS[minOf(state.callCount - 1, PHONE_COOLDOWNS.size - 1)]
        return if (elapsed < cooldown) cooldown - elapsed else null
    }

    fun recordPhoneRequest(phoneNumber: String) {
        val now = Instant.now()
        phoneStates.compute(phoneNumber) { _, existing ->
            if (existing == null) {
                PhoneState(callCount = 1, lastCallTime = now)
            } else {
                val elapsed = now.epochSecond - existing.lastCallTime.epochSecond
                val count = if (elapsed >= PHONE_RESET_SECONDS) 1 else existing.callCount + 1
                PhoneState(callCount = count, lastCallTime = now)
            }
        }
    }

    /**
     * Returns seconds until the window resets if the IP is rate-limited, null if allowed.
     */
    fun checkIpLimit(ip: String): Long? {
        val now = Instant.now()
        val windowStart = now.minusSeconds(IP_WINDOW_SECONDS)
        val timestamps = ipRequests.getOrPut(ip) { ArrayDeque() }
        synchronized(timestamps) {
            timestamps.removeAll { it.isBefore(windowStart) }
            if (timestamps.size >= IP_LIMIT) {
                val oldest = timestamps.first()
                return IP_WINDOW_SECONDS - (now.epochSecond - oldest.epochSecond)
            }
            return null
        }
    }

    fun recordIpRequest(ip: String) {
        val now = Instant.now()
        val timestamps = ipRequests.getOrPut(ip) { ArrayDeque() }
        synchronized(timestamps) {
            val windowStart = now.minusSeconds(IP_WINDOW_SECONDS)
            timestamps.removeAll { it.isBefore(windowStart) }
            timestamps.addLast(now)
        }
    }
}

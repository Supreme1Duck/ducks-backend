package com.ducks.common.ratelimit

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Не больше [limit] запросов с одного ключа (ip) за последние [windowSeconds] секунд.
 *
 * Очередь ключа меняется только внутри compute/computeIfPresent: мапа держит на ключ
 * блокировку, так что удаление опустевшей очереди не теряет запись, пришедшую в тот же момент.
 * Ключи без запросов в окне удаляются, иначе каждый новый ip висел бы в памяти до перезапуска.
 */
class SlidingWindowLimiter(
    private val limit: Int,
    private val windowSeconds: Long,
    private val clock: () -> Instant = Instant::now,
) {

    private val requests = ConcurrentHashMap<String, ArrayDeque<Instant>>()
    private val lastCleanupSecond = AtomicLong(0)

    internal val trackedKeys: Int get() = requests.size

    /** @return сколько секунд ждать, если с этого ключа запросов уже слишком много, иначе null. */
    fun check(key: String): Long? {
        val now = clock()
        var wait: Long? = null

        requests.computeIfPresent(key) { _, timestamps ->
            timestamps.removeExpired(now)
            if (timestamps.size >= limit) {
                wait = windowSeconds - (now.epochSecond - timestamps.first().epochSecond)
            }
            timestamps.removeIfEmpty()
        }
        return wait
    }

    fun record(key: String) {
        val now = clock()

        requests.compute(key) { _, timestamps ->
            (timestamps ?: ArrayDeque()).apply {
                removeExpired(now)
                addLast(now)
            }
        }
        removeInactiveKeysOncePerWindow(now)
    }

    // Ключ, с которого больше не приходят, сам по себе не удалится: раз в окно проходим по всем.
    private fun removeInactiveKeysOncePerWindow(now: Instant) {
        val last = lastCleanupSecond.get()
        if (now.epochSecond - last < windowSeconds) return
        if (!lastCleanupSecond.compareAndSet(last, now.epochSecond)) return

        for (key in requests.keys) {
            requests.computeIfPresent(key) { _, timestamps ->
                timestamps.removeExpired(now)
                timestamps.removeIfEmpty()
            }
        }
    }

    private fun ArrayDeque<Instant>.removeExpired(now: Instant) {
        val windowStart = now.minusSeconds(windowSeconds)
        removeAll { it.isBefore(windowStart) }
    }

    /** null из compute удаляет ключ из мапы. */
    private fun ArrayDeque<Instant>.removeIfEmpty(): ArrayDeque<Instant>? = if (isEmpty()) null else this
}

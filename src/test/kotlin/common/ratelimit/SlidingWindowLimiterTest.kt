package common.ratelimit

import com.ducks.common.ratelimit.SlidingWindowLimiter
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlidingWindowLimiterTest {

    private var now = Instant.parse("2026-09-12T10:00:00Z")
    private val limiter = SlidingWindowLimiter(limit = 2, windowSeconds = 3600, clock = { now })

    @Test
    fun `после окна ключ снова пропускается`() {
        repeat(2) { limiter.record("a") }
        assertEquals(3600, limiter.check("a"))

        now = now.plusSeconds(3601)
        assertNull(limiter.check("a"))
    }

    @Test
    fun `проверка без запросов не заводит ключ`() {
        assertNull(limiter.check("a"))
        assertEquals(0, limiter.trackedKeys)
    }

    @Test
    fun `ключ без запросов в окне удаляется при проверке`() {
        limiter.record("a")

        now = now.plusSeconds(3601)
        assertNull(limiter.check("a"))
        assertEquals(0, limiter.trackedKeys)
    }

    @Test
    fun `ip, с которых больше не приходят, вычищаются раз в окно`() {
        limiter.record("a")
        limiter.record("b")

        now = now.plusSeconds(3601)
        limiter.record("c")
        assertEquals(1, limiter.trackedKeys)
    }
}

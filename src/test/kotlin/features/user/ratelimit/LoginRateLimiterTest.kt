package features.user.ratelimit

import com.ducks.features.user.ratelimit.LoginRateLimiter
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginRateLimiterTest {

    private val limiter = LoginRateLimiter()

    private val ip = "10.0.0.1"
    private val otherIp = "10.0.0.2"

    private fun login(ip: String, times: Int) = repeat(times) { limiter.recordIpRequest(ip) }

    @Test
    fun `первые двадцать входов с ip проходят`() {
        repeat(20) {
            assertNull(limiter.checkIpLimit(ip))
            limiter.recordIpRequest(ip)
        }
    }

    @Test
    fun `двадцать первый вход с того же ip блокируется до конца часа`() {
        login(ip, 20)

        val wait = assertNotNull(limiter.checkIpLimit(ip))
        assertTrue(wait in 1..3600)
    }

    @Test
    fun `лимит одного ip не мешает другому`() {
        login(ip, 20)

        assertNull(limiter.checkIpLimit(otherIp))
    }
}

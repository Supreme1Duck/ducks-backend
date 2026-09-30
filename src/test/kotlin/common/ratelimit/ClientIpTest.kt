package com.ducks.common.ratelimit

import kotlin.test.Test
import kotlin.test.assertEquals

class ClientIpTest {

    @Test
    fun `напрямую заголовок клиента не слушаем`() {
        val ip = clientIp(
            connectionAddress = "93.125.82.179",
            forwardedFor = "1.2.3.4",
            realIp = "5.6.7.8",
        )

        assertEquals("93.125.82.179", ip)
    }

    @Test
    fun `за своим nginx берём X-Real-IP`() {
        val ip = clientIp(
            connectionAddress = "127.0.0.1",
            forwardedFor = "1.2.3.4",
            realIp = "5.6.7.8",
        )

        assertEquals("5.6.7.8", ip)
    }

    @Test
    fun `без X-Real-IP берём первый адрес из X-Forwarded-For`() {
        val ip = clientIp(
            connectionAddress = "::1",
            forwardedFor = " 1.2.3.4 , 10.0.0.1 ",
            realIp = null,
        )

        assertEquals("1.2.3.4", ip)
    }

    @Test
    fun `пустые заголовки не считаются адресом`() {
        val ip = clientIp(
            connectionAddress = "127.0.0.1",
            forwardedFor = "   ",
            realIp = "",
        )

        assertEquals("127.0.0.1", ip)
    }

    @Test
    fun `за прокси без заголовков остаётся адрес соединения`() {
        val ip = clientIp(connectionAddress = "127.0.0.1", forwardedFor = null, realIp = null)

        assertEquals("127.0.0.1", ip)
    }
}

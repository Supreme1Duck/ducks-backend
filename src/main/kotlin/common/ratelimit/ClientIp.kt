package com.ducks.common.ratelimit

import io.ktor.server.request.*

/**
 * Адреса, с которых к нам приходит наш же nginx: он стоит на том же сервере и ходит
 * в Ktor через локальный интерфейс. Появится прокси на отдельной машине — её адрес
 * нужно добавить сюда, иначе все запросы сложатся в одну корзину лимита (заметно будет
 * сразу: лимит начнёт срабатывать на чужие заявки).
 */
private val TRUSTED_PROXY_ADDRESSES = setOf("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")

/**
 * Адрес клиента для счётчиков лимитов — из соединения, а не из заголовка.
 *
 * `X-Forwarded-For` осмыслен только когда его выставляет наш прокси. Напрямую (сервер
 * слушает порт сам, как на деве) заголовок присылает сам клиент: подменяя его на каждый
 * запрос, лимит по ip обходился бы даром.
 *
 * Исключение — соединение от nginx с того же сервера (прод за https://ducks.by): там
 * в соединении всегда loopback, и без заголовка весь интернет попал бы в одну корзину.
 * Клиентский заголовок nginx обязан затирать, а не дополнять:
 *
 *     proxy_set_header X-Real-IP $remote_addr;
 *     proxy_set_header X-Forwarded-For $remote_addr;   # не $proxy_add_x_forwarded_for
 *
 * `X-Real-IP` предпочтительнее: в нём всегда один адрес, и подставить в него клиента
 * нельзя даже при `$proxy_add_x_forwarded_for`.
 *
 * @param connectionAddress адрес, с которого реально пришли байты.
 */
fun clientIp(connectionAddress: String, forwardedFor: String?, realIp: String?): String {
    if (connectionAddress !in TRUSTED_PROXY_ADDRESSES) {
        return connectionAddress
    }

    return realIp?.trim()?.takeIf { it.isNotEmpty() }
        ?: forwardedFor?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
        ?: connectionAddress
}

fun ApplicationRequest.clientIp(): String = clientIp(
    connectionAddress = local.remoteAddress,
    forwardedFor = headers["X-Forwarded-For"],
    realIp = headers["X-Real-IP"],
)

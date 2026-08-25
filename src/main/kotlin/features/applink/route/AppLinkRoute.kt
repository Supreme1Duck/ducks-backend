package com.ducks.features.applink.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap

// Ссылка для QR-кодов: /app определяет платформу по User-Agent и уводит в стор
// с приложением клиента. Публичная, без авторизации — QR сканируют обычной камерой.
// На десктопе и неизвестных платформах отдаём страницу с обеими кнопками,
// вёрстка — src/main/resources/app/download.html.
private const val APP_STORE_URL = "https://apps.apple.com/app/id0000000000"
private const val GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=com.ducks.client"

private const val DOWNLOAD_PAGE_RESOURCE = "app/download.html"

private val cache = ConcurrentHashMap<String, String>()

fun Route.appLinkRoute() {

    get("/app") {
        val userAgent = call.request.userAgent().orEmpty().lowercase()

        // Ссылки в сторах могут поменяться, поэтому редирект временный и без кэша.
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")

        when {
            userAgent.isIos() -> call.respondRedirect(APP_STORE_URL)
            userAgent.isAndroid() -> call.respondRedirect(GOOGLE_PLAY_URL)
            else -> {
                val page = cache.getOrPut(DOWNLOAD_PAGE_RESOURCE) { readDownloadPage() }
                call.respondText(page, ContentType.Text.Html.withCharset(Charsets.UTF_8))
            }
        }
    }
}

private fun String.isIos(): Boolean =
    contains("iphone") || contains("ipad") || contains("ipod")

private fun String.isAndroid(): Boolean = contains("android")

private fun readDownloadPage(): String =
    checkNotNull(object {}.javaClass.classLoader.getResourceAsStream(DOWNLOAD_PAGE_RESOURCE)) {
        "Не найден ресурс страницы загрузки: $DOWNLOAD_PAGE_RESOURCE"
    }.use { it.readBytes().decodeToString() }
        .replace("{{APP_STORE_URL}}", APP_STORE_URL)
        .replace("{{GOOGLE_PLAY_URL}}", GOOGLE_PLAY_URL)

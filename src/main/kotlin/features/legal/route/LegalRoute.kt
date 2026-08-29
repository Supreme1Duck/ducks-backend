package com.ducks.features.legal.route

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap

// Правовые документы отдаём готовым HTML: страницы открываются вебвью
// в мобильном приложении, поэтому они публичные и без авторизации.
// Исходники — src/main/resources/legal/*.md, HTML пересобирается
// скриптом scripts/render-legal-docs.py.

private val cache = ConcurrentHashMap<String, String>()

fun Route.legalRoute() {

    route("/legal") {

        get("privacy-policy") {
            respondDocument("legal/privacy-policy.html")
        }

        get("data-processing") {
            respondDocument("legal/data-processing.html")
        }
    }
}

/**
 * Документ, которого ещё нет в ресурсах, отдаётся как 404: роут можно завести
 * заранее, до того как вёрстка страницы доедет в репозиторий.
 */
private suspend fun RoutingContext.respondDocument(resource: String) {
    val page = cache[resource] ?: readResource(resource)?.also { cache[resource] = it }

    if (page == null) {
        call.respond(HttpStatusCode.NotFound)
        return
    }

    call.respondText(page, ContentType.Text.Html.withCharset(Charsets.UTF_8))
}

private fun readResource(path: String): String? =
    object {}.javaClass.classLoader.getResourceAsStream(path)
        ?.use { it.readBytes().decodeToString() }

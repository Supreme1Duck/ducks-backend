package com.ducks.features.landing.route

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LandingRouteTest {

    @Test
    fun `seller landing is served from root`() = testApplication {
        application { routing { landingRoute() } }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsText().contains("<!DOCTYPE html>", ignoreCase = true))
        assertTrue(response.bodyAsText().contains("Утки для кофеен"))
        assertTrue(response.bodyAsText().contains("id=\"lead\""))
        assertTrue(response.bodyAsText().contains("assets/seller-barista-duck.jpg"))
    }

    @Test
    fun `old landing paths redirect to root`() = testApplication {
        application { routing { landingRoute() } }

        val client = createClient { followRedirects = false }

        for (path in listOf("/dark", "/business.html")) {
            val response = client.get(path)

            assertEquals(HttpStatusCode.MovedPermanently, response.status, path)
            assertEquals("/", response.headers[HttpHeaders.Location], path)
        }
    }

    @Test
    fun `only the page and its assets are exposed`() = testApplication {
        application { routing { landingRoute() } }

        // Сама страница живёт на «/»: по имени файла её не отдаём, как и что-либо ещё из landing.
        assertEquals(HttpStatusCode.NotFound, client.get("/index.html").status)
    }

    @Test
    fun `search engines get robots and sitemap`() = testApplication {
        application { routing { landingRoute() } }

        val robots = client.get("/robots.txt")

        assertEquals(HttpStatusCode.OK, robots.status)
        assertTrue(robots.bodyAsText().contains("Sitemap: https://ducks.by/sitemap.xml"))

        val sitemap = client.get("/sitemap.xml")

        assertEquals(HttpStatusCode.OK, sitemap.status)
        assertTrue(sitemap.bodyAsText().contains("<loc>https://ducks.by/</loc>"))
    }

    // Разметка для поисковика: ломать её опечаткой в json незаметно, поэтому проверяем разбором.
    @Test
    fun `page carries valid structured data`() = testApplication {
        application { routing { landingRoute() } }

        val html = client.get("/").bodyAsText()
        val json = Regex("""<script type="application/ld\+json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)

        requireNotNull(json) { "На странице нет разметки ld+json" }

        val data = Json.parseToJsonElement(json).jsonObject

        assertEquals("SoftwareApplication", data.getValue("@type").jsonPrimitive.content)
        assertEquals("https://ducks.by/", data.getValue("url").jsonPrimitive.content)
    }

    @Test
    fun `assets are served next to the page`() = testApplication {
        application { routing { landingRoute() } }

        val response = client.get("/assets/logo-duck-mark.png")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Image.PNG, response.contentType()?.withoutParameters())
    }

    // Telegram берёт картинку превью только по абсолютному урлу, поэтому в вёрстке
    // стоит домен прода. Проверяем, что за этим урлом лежит реальный файл из assets.
    @Test
    fun `preview images point to served assets`() = testApplication {
        application { routing { landingRoute() } }

        for (page in listOf("/")) {
            val html = client.get(page).bodyAsText()
            val image = Regex("""<meta property="og:image" content="https://ducks\.by(/[^"]+)">""")
                .find(html)?.groupValues?.get(1)

            requireNotNull(image) { "На $page нет og:image с абсолютным урлом" }

            val response = client.get(image)

            assertEquals(HttpStatusCode.OK, response.status, image)
            assertEquals(ContentType.Image.JPEG, response.contentType()?.withoutParameters(), image)
        }
    }

    @Test
    fun `every asset referenced by the page is served`() = testApplication {
        application { routing { landingRoute() } }

        val html = client.get("/").bodyAsText()
        val assets = Regex("""(?:src|href)="(assets/[^"]+)"|url\("(assets/[^"]+)"\)""")
            .findAll(html)
            .map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
            .toSet()

        assertTrue(assets.isNotEmpty())
        for (asset in assets) {
            assertEquals(HttpStatusCode.OK, client.get("/$asset").status, asset)
        }
    }

    @Test
    fun `unknown path is not found`() = testApplication {
        application { routing { landingRoute() } }

        assertEquals(HttpStatusCode.NotFound, client.get("/assets/nothing.png").status)
    }
}

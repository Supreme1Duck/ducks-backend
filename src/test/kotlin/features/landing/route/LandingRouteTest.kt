package com.ducks.features.landing.route

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LandingRouteTest {

    @Test
    fun `landing is served from root`() = testApplication {
        application { routing { landingRoute() } }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsText().contains("<!DOCTYPE html>", ignoreCase = true))
    }

    @Test
    fun `business page is served next to the guest page`() = testApplication {
        application { routing { landingRoute() } }

        val response = client.get("/business.html")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsText().contains("Утки для кофеен"))
    }

    @Test
    fun `assets are served next to the page`() = testApplication {
        application { routing { landingRoute() } }

        val response = client.get("/assets/logo-duck.png")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Image.PNG, response.contentType()?.withoutParameters())
    }

    @Test
    fun `unknown path is not found`() = testApplication {
        application { routing { landingRoute() } }

        assertEquals(HttpStatusCode.NotFound, client.get("/assets/nothing.png").status)
    }
}

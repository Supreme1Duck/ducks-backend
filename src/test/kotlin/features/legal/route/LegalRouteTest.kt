package com.ducks.features.legal.route

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegalRouteTest {

    @Test
    fun `privacy policy is served as html`() = testApplication {
        application { routing { legalRoute() } }

        val response = client.get("/legal/privacy-policy")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())

        val body = response.bodyAsText()
        assertTrue(body.startsWith("<!DOCTYPE html>"))
        assertTrue(body.contains("ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ"))
    }

    @Test
    fun `unknown document is not found`() = testApplication {
        application { routing { legalRoute() } }

        assertEquals(HttpStatusCode.NotFound, client.get("/legal/terms-of-use").status)
    }
}

package com.ducks.common.geo

import com.ducks.util.DucksBadRequestError
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoPointTest {

    // Минск: площадь Победы и Немига, между ними по прямой ~1.7 км.
    private val victorySquare = GeoPoint(latitude = 53.9080, longitude = 27.5750)
    private val nemiga = GeoPoint(latitude = 53.9020, longitude = 27.5520)

    @Test
    fun `distance to itself is zero`() {
        assertEquals(0.0, victorySquare.distanceMetersTo(victorySquare))
    }

    @Test
    fun `distance is symmetric`() {
        assertEquals(
            victorySquare.distanceMetersTo(nemiga),
            nemiga.distanceMetersTo(victorySquare),
        )
    }

    @Test
    fun `approximation stays within half a percent of haversine`() {
        val pairs = listOf(
            victorySquare to nemiga,                                   // соседние районы, ~1.6 км
            victorySquare to GeoPoint(53.8845, 27.5375),               // через весь центр
            victorySquare to GeoPoint(53.6800, 23.8300),               // Минск — Гродно, ~240 км
        )

        pairs.forEach { (from, to) ->
            val approximate = from.distanceMetersTo(to)
            val exact = haversineMeters(from, to)

            assertTrue(
                abs(approximate - exact) / exact < 0.005,
                "haversine дал $exact м, приближение — $approximate м",
            )
        }
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        val distance = GeoPoint(53.0, 27.5).distanceMetersTo(GeoPoint(54.0, 27.5))

        assertTrue(abs(distance - 111_195.0) < 100.0, "получили $distance м")
    }

    @Test
    fun `nearer shop has smaller distance`() {
        val nearShop = GeoPoint(latitude = 53.9085, longitude = 27.5755)

        assertTrue(victorySquare.distanceMetersTo(nearShop) < victorySquare.distanceMetersTo(nemiga))
    }

    @Test
    fun `no coordinates at all means no sorting by distance`() {
        assertNull(GeoPoint.parse(latitude = null, longitude = null))
    }

    @Test
    fun `half of a pair is a bad request`() {
        assertFailsWith<DucksBadRequestError> { GeoPoint.parse(latitude = 53.9, longitude = null) }
        assertFailsWith<DucksBadRequestError> { GeoPoint.parse(latitude = null, longitude = 27.5) }
    }

    @Test
    fun `coordinates out of range are rejected`() {
        assertFailsWith<DucksBadRequestError> { GeoPoint.parse(latitude = 91.0, longitude = 27.5) }
        assertFailsWith<DucksBadRequestError> { GeoPoint.parse(latitude = 53.9, longitude = 180.5) }
        assertFailsWith<DucksBadRequestError> { GeoPoint.parse(latitude = Double.NaN, longitude = 27.5) }
    }

    @Test
    fun `edges of the range are valid`() {
        assertEquals(GeoPoint(90.0, 180.0), GeoPoint.parse(latitude = 90.0, longitude = 180.0))
        assertEquals(GeoPoint(-90.0, -180.0), GeoPoint.parse(latitude = -90.0, longitude = -180.0))
    }

    @Test
    fun `query param must be a number`() {
        assertEquals(53.9, "53.9".toDegreesOrThrow("lat"))
        assertFailsWith<DucksBadRequestError> { "рядом".toDegreesOrThrow("lat") }
    }

    // Честный haversine — эталон, с которым сверяется упрощённая формула.
    private fun haversineMeters(from: GeoPoint, to: GeoPoint): Double {
        val earthRadiusMeters = 6_371_000.0

        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(fromLat) * cos(toLat) * sin(dLon / 2) * sin(dLon / 2)

        return earthRadiusMeters * 2 * asin(sqrt(a))
    }
}

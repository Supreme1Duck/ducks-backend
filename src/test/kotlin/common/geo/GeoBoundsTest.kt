package com.ducks.common.geo

import com.ducks.util.DucksBadRequestError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoBoundsTest {

    // Прямоугольник вокруг центра Минска.
    private val south = 53.8845
    private val west = 27.5375
    private val north = 53.9200
    private val east = 27.6100

    @Test
    fun `пустой набор границ — это отсутствие границ`() {
        assertNull(GeoBounds.parse(null, null, null, null))
    }

    @Test
    fun `неполный набор границ — ошибка запроса`() {
        assertFailsWith<DucksBadRequestError> {
            GeoBounds.parse(south, west, north, null)
        }
    }

    @Test
    fun `углы разбираются как есть`() {
        val bounds = GeoBounds.parse(south, west, north, east)!!

        assertEquals(GeoPoint(south, west), bounds.southWest)
        assertEquals(GeoPoint(north, east), bounds.northEast)
    }

    @Test
    fun `юг севернее севера — ошибка запроса`() {
        assertFailsWith<DucksBadRequestError> {
            GeoBounds.parse(north, west, south, east)
        }
    }

    @Test
    fun `координаты за пределами градусной сетки — ошибка запроса`() {
        assertFailsWith<DucksBadRequestError> {
            GeoBounds.parse(south, west, 91.0, east)
        }
    }

    @Test
    fun `обычная область не пересекает 180-й меридиан`() {
        val bounds = GeoBounds.parse(south, west, north, east)!!

        assertFalse(bounds.crossesAntimeridian)
    }

    @Test
    fun `область с западом правее востока пересекает 180-й меридиан`() {
        // Экран над Беринговым проливом: слева Чукотка, справа Аляска.
        val bounds = GeoBounds.parse(64.0, 179.0, 66.0, -179.0)!!

        assertTrue(bounds.crossesAntimeridian)
    }
}

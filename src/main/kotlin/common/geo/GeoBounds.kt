package com.ducks.common.geo

import com.ducks.util.DucksBadRequestError

/**
 * Видимый прямоугольник карты (WGS84) — то, что клиент сейчас показывает на экране.
 *
 * Задаётся двумя углами, юго-западным и северо-восточным, ровно как их отдают
 * картографические sdk на телефоне: клиенту не нужно ничего пересчитывать перед запросом.
 */
data class GeoBounds(
    val southWest: GeoPoint,
    val northEast: GeoPoint,
) {

    /**
     * Область перехлёстывает 180-й меридиан: западный угол оказался правее восточного.
     * Тогда по долготе это два куска — от запада до 180 и от -180 до востока, — и
     * условие отбора превращается из "и" в "или".
     */
    val crossesAntimeridian: Boolean
        get() = southWest.longitude > northEast.longitude

    companion object {

        /**
         * Углы приходят только вчетвером: неполный набор — ошибка запроса, а не молчаливый
         * показ всех кофеен подряд (иначе клиент с поломанным запросом получит не ту карту
         * и не узнает об этом).
         */
        fun parse(
            southLatitude: Double?,
            westLongitude: Double?,
            northLatitude: Double?,
            eastLongitude: Double?,
        ): GeoBounds? {
            val corners = listOf(southLatitude, westLongitude, northLatitude, eastLongitude)

            if (corners.all { it == null }) {
                return null
            }

            if (corners.any { it == null }) {
                throw DucksBadRequestError("Границы карты передаются целиком: нужны оба угла, юго-западный и северо-восточный.")
            }

            val southWest = GeoPoint.parse(southLatitude, westLongitude)!!
            val northEast = GeoPoint.parse(northLatitude, eastLongitude)!!

            // По широте перехлёста не бывает: полюса не соединяются, юг всегда ниже севера.
            if (southWest.latitude > northEast.latitude) {
                throw DucksBadRequestError("Южная граница карты не может быть севернее северной.")
            }

            return GeoBounds(southWest = southWest, northEast = northEast)
        }
    }
}

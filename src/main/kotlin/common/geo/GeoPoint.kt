package com.ducks.common.geo

import com.ducks.util.DucksBadRequestError
import kotlin.math.cos
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Точка на карте в градусах WGS84 — то же, что отдаёт геолокация телефона.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {

    /**
     * Расстояние по прямой в метрах.
     *
     * Равнопромежуточная проекция вместо haversine: на городских расстояниях
     * расхождение меньше половины процента, зато формула повторяется один в один
     * в sql (см. CoffeeShopDistanceExpression), и порядок в выдаче всегда совпадает
     * с показанными пользователю километрами.
     */
    fun distanceMetersTo(other: GeoPoint): Double {
        val dLatRadians = Math.toRadians(latitude - other.latitude)
        val dLonRadians = Math.toRadians(longitude - other.longitude)
        val middleLatRadians = Math.toRadians((latitude + other.latitude) / 2)

        val x = dLonRadians * cos(middleLatRadians)

        return EARTH_RADIUS_METERS * sqrt(dLatRadians * dLatRadians + x * x)
    }

    companion object {

        /**
         * Координаты приходят только парой: половина пары — ошибка запроса, а не
         * молчаливый откат к сортировке по умолчанию (иначе клиент с поломанной
         * геолокацией не узнает, что список ему пришёл не тот).
         */
        fun parse(latitude: Double?, longitude: Double?): GeoPoint? {
            if (latitude == null && longitude == null) {
                return null
            }

            if (latitude == null || longitude == null) {
                throw DucksBadRequestError("Координаты передаются парой: нужны и широта, и долгота.")
            }

            if (latitude !in -90.0..90.0) {
                throw DucksBadRequestError("Широта должна быть в диапазоне от -90 до 90.")
            }

            if (longitude !in -180.0..180.0) {
                throw DucksBadRequestError("Долгота должна быть в диапазоне от -180 до 180.")
            }

            return GeoPoint(latitude = latitude, longitude = longitude)
        }
    }
}

fun String.toDegreesOrThrow(paramName: String): Double {
    return toDoubleOrNull() ?: throw DucksBadRequestError("Параметр $paramName должен быть числом.")
}

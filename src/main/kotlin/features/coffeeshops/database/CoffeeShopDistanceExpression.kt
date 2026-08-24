package com.ducks.features.coffeeshops.database

import com.ducks.common.geo.GeoPoint
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.QueryBuilder

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Расстояние от точки до кофейни в метрах — для ORDER BY, чтобы отбор ближайших
 * делала база, а не приложение поверх выкачанной таблицы.
 *
 * Формула — равнопромежуточная проекция, ровно как в [GeoPoint.distanceMetersTo]:
 * обычная арифметика, без postgis и расширений в базе.
 *
 * Для кофеен без координат выражение даёт null, и в postgres при ASC они встают
 * в конец выдачи; на всякий случай порядок закрепляется через ASC_NULLS_LAST.
 */
class CoffeeShopDistanceExpression(
    private val from: GeoPoint,
) : Expression<Double>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append("($EARTH_RADIUS_METERS * sqrt(")

            append("power(radians(")
            append(CoffeeShopTable.latitude)
            append(" - ")
            registerArgument(DoubleColumnType(), from.latitude)
            append("), 2)")

            append(" + ")

            append("power(radians(")
            append(CoffeeShopTable.longitude)
            append(" - ")
            registerArgument(DoubleColumnType(), from.longitude)
            append(") * cos(radians((")
            append(CoffeeShopTable.latitude)
            append(" + ")
            registerArgument(DoubleColumnType(), from.latitude)
            append(") / 2)), 2)")

            append("))")
        }
    }
}

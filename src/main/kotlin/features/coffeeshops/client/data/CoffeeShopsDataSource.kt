package com.ducks.features.coffeeshops.client.data

import com.ducks.common.geo.GeoBounds
import com.ducks.common.geo.GeoPoint
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopMapPinDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopPreviewDTO
import com.ducks.features.coffeeshops.database.CoffeeShopDistanceExpression
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopMapPin
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopPreview
import com.ducks.features.orders.data.model.WorkTimeModel
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.jdbc.selectAll

class CoffeeShopsDataSource {

    fun exists(
        shopId: Long,
    ) : Boolean {
        return CoffeeShopTable.selectAll()
            .where { CoffeeShopTable.id eq shopId }
            .empty()
            .not()
    }

    fun getAllShops(
        lastId: Long?,
        limit: Int?,
    ): List<CoffeeShopPreviewDTO> {
        return CoffeeShopTable
            .selectAll()
            .where(CoffeeShopTable.id less (lastId ?: Long.MAX_VALUE))
            .orderBy(CoffeeShopTable.id, SortOrder.DESC)
            .limit(limit ?: Int.MAX_VALUE)
            .map {
                it.mapToCoffeeShopPreview()
            }
    }

    /**
     * То же, что [getAllShops], но по возрастанию расстояния до [userLocation].
     *
     * Курсор по lastId здесь не работает — порядок задаёт не id, — поэтому листается
     * offset'ом. Кофейни без координат встают в конец, между собой сортируются как и
     * в обычном списке, по убыванию id.
     */
    fun getAllShopsNearby(
        userLocation: GeoPoint,
        offset: Long?,
        limit: Int?,
    ): List<CoffeeShopPreviewDTO> {
        val distance = CoffeeShopDistanceExpression(userLocation)

        return CoffeeShopTable
            .selectAll()
            .orderBy(
                distance to SortOrder.ASC_NULLS_LAST,
                CoffeeShopTable.id to SortOrder.DESC,
            )
            .limit(limit ?: Int.MAX_VALUE)
            .offset(offset ?: 0)
            .map {
                it.mapToCoffeeShopPreview(userLocation)
            }
    }

    /**
     * Кофейни для карты: только те, у кого проставлены координаты, — метку без точки
     * поставить некуда.
     *
     * [bounds] — видимая область экрана. Без неё отдаются все кофейни с координатами:
     * так клиент может один раз забрать карту целиком, если город небольшой.
     *
     * Порядок важен только тем, что [limit] обрезает хвост: если координаты клиента
     * известны, первыми в выдачу идут ближайшие к нему — обрезается тогда дальнее.
     */
    fun getShopsOnMap(
        bounds: GeoBounds?,
        userLocation: GeoPoint?,
        limit: Int,
    ): List<CoffeeShopMapPinDTO> {
        val order = if (userLocation != null) {
            CoffeeShopDistanceExpression(userLocation) to SortOrder.ASC
        } else {
            CoffeeShopTable.id to SortOrder.DESC
        }

        return CoffeeShopTable
            .selectAll()
            .where { hasCoordinates() and insideBounds(bounds) }
            .orderBy(order)
            .limit(limit)
            .mapNotNull {
                it.mapToCoffeeShopMapPin(userLocation)
            }
    }

    private fun SqlExpressionBuilder.hasCoordinates(): Op<Boolean> {
        return CoffeeShopTable.latitude.isNotNull() and CoffeeShopTable.longitude.isNotNull()
    }

    private fun SqlExpressionBuilder.insideBounds(bounds: GeoBounds?): Op<Boolean> {
        if (bounds == null) return Op.TRUE

        val insideLatitude = (CoffeeShopTable.latitude greaterEq bounds.southWest.latitude) and
                (CoffeeShopTable.latitude lessEq bounds.northEast.latitude)

        // Область через 180-й меридиан состоит из двух кусков, и попадание в неё — это
        // "либо правее западной границы, либо левее восточной", а не "между ними".
        val insideLongitude = if (bounds.crossesAntimeridian) {
            (CoffeeShopTable.longitude greaterEq bounds.southWest.longitude) or
                    (CoffeeShopTable.longitude lessEq bounds.northEast.longitude)
        } else {
            (CoffeeShopTable.longitude greaterEq bounds.southWest.longitude) and
                    (CoffeeShopTable.longitude lessEq bounds.northEast.longitude)
        }

        return insideLatitude and insideLongitude
    }

    fun getShopDetails(
        shopId: Long,
        workTime: WorkTimeModel?,
    ) : CoffeeShopDetailsDTO {
        return CoffeeShopTable
            .selectAll()
            .where {
                CoffeeShopTable.id eq shopId
            }.map {
                it.mapToCoffeeShopDetailsDTO(workTime)
            }.first()
    }
}
package com.ducks.features.coffeeshops.client.domain

import com.ducks.common.geo.GeoBounds
import com.ducks.common.geo.GeoPoint
import com.ducks.features.coffeeshops.checkShopIsNotTemporaryClosed
import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.CoffeeShopsDataSource
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopWithProductsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.OrderTimeDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopMapPinDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopPreviewDTO
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class CoffeeShopsRepository(
    private val shopsDataSource: CoffeeShopsDataSource,
    private val productDataSource: CoffeeProductsDataSource,
    private val fetchAvailableOrdersTimeListRepository: FetchAvailableOrdersTimeListRepository,
) {

    suspend fun exists(shopId: Long): Boolean {
        return newSuspendedTransaction {
            shopsDataSource.exists(shopId)
        }
    }

    suspend fun getShop(shopId: Long): CoffeeShopWithProductsDTO {
        return newSuspendedTransaction {
            val workTime = fetchAvailableOrdersTimeListRepository.findShopsCurrentWorkTime(shopId)
            val shop = shopsDataSource.getShopDetails(shopId, workTime)

            val productsWithCategories = productDataSource.fetchByShop(shopId)

            CoffeeShopWithProductsDTO(
                shop = shop,
                products = productsWithCategories
            )
        }
    }

    /**
     * Список кофеен. Если клиент прислал свои координаты — по возрастанию расстояния
     * от него (тогда листается через offset), иначе прежний курсор по lastId.
     */
    suspend fun getShopsList(
        lastId: Long?,
        limit: Int?,
        offset: Long?,
        userLocation: GeoPoint?,
    ): List<CoffeeShopPreviewDTO> {
        return newSuspendedTransaction {
            val shops = if (userLocation != null) {
                shopsDataSource.getAllShopsNearby(userLocation, offset, limit)
            } else {
                shopsDataSource.getAllShops(lastId, limit)
            }
            shops.map { shop ->
                val workTime = fetchAvailableOrdersTimeListRepository.findShopsCurrentWorkTime(shop.id)
                shop.copy(
                    openTime = workTime?.startTime,
                    closeTime = workTime?.endTime,
                    isClosed = workTime?.isClosed ?: true,
                )
            }
        }
    }

    suspend fun getShopsForMap(
        bounds: GeoBounds?,
        userLocation: GeoPoint?,
        limit: Int?,
    ): List<CoffeeShopMapPinDTO> {
        val pinsLimit = (limit ?: DEFAULT_MAP_PINS_LIMIT).coerceIn(1, MAX_MAP_PINS_LIMIT)

        return newSuspendedTransaction {
            val pins = shopsDataSource.getShopsOnMap(
                bounds = bounds,
                userLocation = userLocation,
                limit = pinsLimit,
            )

            val workTimes = fetchAvailableOrdersTimeListRepository
                .findShopsCurrentWorkTime(pins.map { it.id })

            pins.map { pin ->
                val workTime = workTimes[pin.id]
                pin.copy(
                    openTime = workTime?.startTime,
                    closeTime = workTime?.endTime,
                    isClosed = workTime?.isClosed ?: true,
                )
            }
        }
    }

    suspend fun getOrdersTimeList(
        shopId: Long,
        productIds: List<Long>,
    ): OrderTimeDTO {
        return newSuspendedTransaction {
            checkShopIsNotTemporaryClosed(shopId)

            val minutesToCook = productDataSource.calculateMinutesToCook(productIds)
            val timestamps = fetchAvailableOrdersTimeListRepository.availableFinishTimes(
                shopId = shopId,
                estimatedOrderFinishTimeInMinutes = minutesToCook,
            ) ?: throw DucksBadRequestError("У кофешопа нет свободного время для заказа.")
            OrderTimeDTO(availableTimestamps = timestamps)
        }
    }

    private companion object {

        // Столько меток экран показывает без тормозов, а город целиком в них помещается
        // с запасом. Клиент, который не прислал границы, всё равно не останется без карты.
        const val DEFAULT_MAP_PINS_LIMIT = 200
        const val MAX_MAP_PINS_LIMIT = 500
    }
}

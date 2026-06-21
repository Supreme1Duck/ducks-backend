package com.ducks.features.coffeeshops.client.domain

import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.CoffeeShopsDataSource
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopWithProductsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.OrderTimeDTO
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

    suspend fun getShopsList(lastId: Long?, limit: Int?): List<CoffeeShopPreviewDTO> {
        return newSuspendedTransaction {
            val shops = shopsDataSource.getAllShops(lastId, limit)
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

    suspend fun getOrdersTimeList(
        shopId: Long,
        productIds: List<Long>,
    ): OrderTimeDTO {
        return newSuspendedTransaction {
            val minutesToCook = productDataSource.estimateCookingTime(productIds).minutesToCook
            val timestamps = fetchAvailableOrdersTimeListRepository.invoke(
                shopId = shopId,
                estimatedOrderFinishTimeInMinutes = minutesToCook,
            ) ?: throw DucksBadRequestError("У кофешопа нет свободного время для заказа.")
            OrderTimeDTO(availableTimestamps = timestamps)
        }
    }
}
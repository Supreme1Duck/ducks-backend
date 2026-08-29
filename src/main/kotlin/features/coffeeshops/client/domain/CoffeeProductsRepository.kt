package com.ducks.features.coffeeshops.client.domain

import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CookingTimeEstimateDTO
import com.ducks.features.coffeeshops.client.data.model.dto.ShopProductPair
import com.ducks.features.coffeeshops.client.routings.request.EstimateCookingTimeRequest
import com.ducks.features.orders.data.repository.FetchAvailableOrdersTimeListRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class CoffeeProductsRepository(
    private val dataSource: CoffeeProductsDataSource,
    private val availableOrdersTimeListRepository: FetchAvailableOrdersTimeListRepository,
) {

    suspend fun getProduct(productId: Long): CoffeeProductWithDetailsDTO {
        return newSuspendedTransaction {
            dataSource.getProductDetails(productId)
        }
    }

    suspend fun findMissingPairs(pairs: List<ShopProductPair>): List<ShopProductPair> {
        return newSuspendedTransaction {
            dataSource.findMissingPairs(pairs)
        }
    }

    suspend fun estimateCookingTime(request: EstimateCookingTimeRequest): CookingTimeEstimateDTO {
        return newSuspendedTransaction {
            val minutesToCook = dataSource.calculateMinutesToCook(request.productIds)

            val estimatedFinishTime = availableOrdersTimeListRepository
                .availableFinishTimes(request.shopId, minutesToCook)
                ?.firstOrNull()
            val minutesToFinish = estimatedFinishTime?.let {
                ((it - Clock.System.now().toEpochMilliseconds()) / 60_000L).toInt().coerceAtLeast(0)
            }

            CookingTimeEstimateDTO(
                minutesToCook = minutesToCook,
                estimatedFinishTime = estimatedFinishTime,
                minutesToFinish = minutesToFinish,
            )
        }
    }
}
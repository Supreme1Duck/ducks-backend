package com.ducks.features.coffeeshops.client.domain

import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CookingTimeEstimateDTO
import com.ducks.features.coffeeshops.client.routings.request.EstimateCookingTimeRequest
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class CoffeeProductsRepository(
    private val dataSource: CoffeeProductsDataSource,
) {

    suspend fun getProduct(productId: Long): CoffeeProductWithDetailsDTO {
        return newSuspendedTransaction {
            dataSource.getProductDetails(productId)
        }
    }

    suspend fun estimateCookingTime(request: EstimateCookingTimeRequest): CookingTimeEstimateDTO {
        return newSuspendedTransaction {
            dataSource.estimateCookingTime(request.productIds)
        }
    }
}
package com.ducks.coffeeshops.common.domain

import com.ducks.coffeeshops.common.data.CoffeeProductsDataSource
import com.ducks.coffeeshops.common.data.model.dto.CoffeeProductWithDetailsDTO
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class CoffeeProductsRepository(
    private val dataSource: CoffeeProductsDataSource,
) {

    suspend fun getProduct(productId: Long): CoffeeProductWithDetailsDTO {
        return newSuspendedTransaction {
            dataSource.getProductDetails(productId)
        }
    }
}
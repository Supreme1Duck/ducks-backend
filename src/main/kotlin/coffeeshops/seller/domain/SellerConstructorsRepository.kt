package com.ducks.coffeeshops.seller.domain

import com.ducks.coffeeshops.seller.data.SellerCoffeeConstructorsDataSource
import com.ducks.coffeeshops.seller.data.model.SellerCoffeeCategoriesWithConstructorsDTO
import com.ducks.coffeeshops.seller.routings.request.constructor.CreateConstructorCategoryRequest
import com.ducks.coffeeshops.seller.routings.request.constructor.CreateConstructorRequest
import com.ducks.coffeeshops.seller.routings.request.constructor.DeleteConstructorRequest
import com.ducks.coffeeshops.seller.routings.request.constructor.SetInStockRequest
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class SellerConstructorsRepository(
    private val dataSource: SellerCoffeeConstructorsDataSource
) {

    suspend fun fetchByShop(shopId: Long): SellerCoffeeCategoriesWithConstructorsDTO {
        return newSuspendedTransaction {
            dataSource.fetchByShop(shopId = shopId)
        }
    }

    suspend fun insertBasic(shopId: Long) {
        return newSuspendedTransaction {
            dataSource.insertBasic(shopId = shopId)
        }
    }

    suspend fun insertNewCategory(
        shopId: Long,
        request: CreateConstructorCategoryRequest,
    ): Long {
        return newSuspendedTransaction {
            dataSource.insertNewCategory(shopId = shopId, request = request)
        }
    }

    suspend fun insertNewConstructor(
        shopId: Long,
        request: CreateConstructorRequest
    ): Long {
        return suspendTransaction {
            dataSource.insertNew(shopId = shopId, request = request)
        }
    }

    suspend fun deleteCategory(
        shopId: Long, categoryId: Long,
    ) {
        return newSuspendedTransaction {
            dataSource.deleteCategory(
                shopId = shopId,
                id = categoryId,
            )
        }
    }

    suspend fun deleteConstructor(
        request: DeleteConstructorRequest,
    ) {
        return newSuspendedTransaction {
            dataSource.deleteConstructor(
                request = request
            )
        }
    }

    suspend fun setInStock(
        setInStockRequest: SetInStockRequest,
    ) {
        return newSuspendedTransaction {
            dataSource.setInStock(setInStockRequest)
        }
    }
}
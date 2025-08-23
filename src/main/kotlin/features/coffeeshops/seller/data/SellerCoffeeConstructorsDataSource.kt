package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.database.CoffeeConstructorCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.database.mappers.mapToConstructorCategoryDTO
import com.ducks.features.coffeeshops.database.mappers.mapToConstructorDTO
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeCategoriesWithConstructorsDTO
import com.ducks.coffeeshops.seller.routings.request.constructor.*
import com.ducks.features.coffeeshops.seller.routings.request.constructor.CreateConstructorRequest
import com.ducks.features.coffeeshops.seller.routings.request.constructor.DeleteConstructorRequest
import com.ducks.features.coffeeshops.seller.routings.request.constructor.SetInStockRequest
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*

class SellerCoffeeConstructorsDataSource {

    fun insertBasic(shopId: Long) {
        basicConstructorsList.forEach { category ->
            val containerId = CoffeeConstructorCategoryTable.insertAndGetId { table ->
                table[CoffeeConstructorCategoryTable.shopId] = shopId

                table[name] = category.name
                table[maxSelection] = category.maxSelection
                table[defaultConstructorId] = category.defaultConstructorId
            }

            category.constructors.forEach { constructor ->
                CoffeeConstructorsTable.insert { table ->
                    table[CoffeeConstructorsTable.shopId] = shopId
                    table[categoryId] = containerId

                    table[name] = constructor.name
                    table[isInStock] = constructor.isInStock
                }
            }
        }
    }

    fun insertNewCategory(
        shopId: Long,
        request: CreateConstructorCategoryRequest,
    ): Long {
        return CoffeeConstructorCategoryTable.insertAndGetId { table ->
            table[CoffeeConstructorCategoryTable.shopId] = shopId
            table[name] = request.name
            table[defaultConstructorId] = request.defaultConstructorId
            table[maxSelection] = request.maxSelection
        }.value
    }

    fun insertNew(
        shopId: Long,
        request: CreateConstructorRequest
    ): Long {
        return CoffeeConstructorsTable
            .insertAndGetId { table ->
                table[CoffeeConstructorsTable.shopId] = shopId

                table[name] = request.name
                table[categoryId] = request.categoryId
                table[isInStock] = request.isInStock
                table[price] = request.price
            }.value
    }

    fun deleteCategory(
        shopId: Long,
        id: Long
    ) {
        CoffeeConstructorsTable.deleteWhere {
            (CoffeeConstructorCategoryTable.shopId eq shopId) and (CoffeeConstructorCategoryTable.id eq id)
        }
    }

    fun deleteConstructor(
        request: DeleteConstructorRequest,
    ) {
        CoffeeConstructorsTable.deleteWhere {
            (CoffeeConstructorsTable.id eq request.categoryId) and (categoryId eq request.containerId)
        }
    }

    fun fetchByShop(
        shopId: Long,
    ): SellerCoffeeCategoriesWithConstructorsDTO {
        val map = CoffeeConstructorCategoryTable
            .join(CoffeeConstructorsTable, joinType = JoinType.LEFT, onColumn = CoffeeConstructorCategoryTable.id, CoffeeConstructorsTable.categoryId)
            .selectAll()
            .where {
                CoffeeConstructorCategoryTable.shopId eq shopId
            }
            .map {
                val category = it.mapToConstructorCategoryDTO()
                val constructor = it.mapToConstructorDTO()

                category to constructor
            }
            .associate {
                it.first to it.second
            }

        return SellerCoffeeCategoriesWithConstructorsDTO(map)
    }

    fun setInStock(
        request: SetInStockRequest
    ) {
        CoffeeConstructorsTable
            .update(where = { CoffeeConstructorsTable.id eq request.constructorId }) {
                it[isInStock] = request.isInStock
            }
    }

    private val basicConstructorsList = listOf(
        Category(
            name = "Базовые добавки",
            maxSelection = null,
            defaultConstructorId = null,
            constructors = listOf(
                Constructor(
                    name = "Сахар", isInStock = true,
                ),
                Constructor(
                    name = "Двойной сахар", isInStock = true,
                ),
                Constructor(
                    name = "Корица", isInStock = true,
                ),
            )
        ),

        Category(
            name = "Сиропы",
            maxSelection = 2,
            defaultConstructorId = null,
            constructors = listOf(
                Constructor(
                    name = "Ванильный", isInStock = true,
                ),
                Constructor(
                    name = "Двойной сахар", isInStock = true,
                ),
                Constructor(
                    name = "Корица", isInStock = true,
                ),
            )
        ),

        Category(
            name = "Молоко",
            maxSelection = 1,
            defaultConstructorId = null,
            constructors = listOf(
                Constructor(
                    name = "Стандартное", isInStock = true,
                ),
                Constructor(
                    name = "Кокосовое молоко", isInStock = true,
                ),
                Constructor(
                    name = "Миндальное молоко", isInStock = true,
                ),
            )
        ),
    )

    private data class Category(
        val name: String,
        val maxSelection: Int?,
        val defaultConstructorId: Long?,
        val constructors: List<Constructor>,
    )

    private data class Constructor(
        val name: String,
        val isInStock: Boolean,
    )
}
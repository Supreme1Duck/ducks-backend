package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.seller.routings.request.constructor.CreateConstructorCategoryRequest
import com.ducks.features.coffeeshops.database.CoffeeConstructorCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.database.mappers.mapToConstructorCategoryDTO
import com.ducks.features.coffeeshops.database.mappers.mapToConstructorDTO
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeCategoriesWithConstructorsDTO
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeCategoryDTO
import com.ducks.features.coffeeshops.seller.routings.request.constructor.CreateConstructorRequest
import com.ducks.features.coffeeshops.seller.routings.request.constructor.DeleteConstructorRequest
import com.ducks.features.coffeeshops.seller.routings.request.constructor.SetInStockRequest
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SellerCoffeeConstructorsDataSource {

    fun insertBasic(shopId: Long) {
        try {
            basicConstructorsList.forEach { category ->
                val containerId = CoffeeConstructorCategoryTable.insertAndGetId { table ->
                    table[CoffeeConstructorCategoryTable.shopId] = shopId

                    table[name] = category.name
                }.value

                category.constructors.map { constructor ->
                    CoffeeConstructorsTable.insertAndGetId { table ->
                        table[CoffeeConstructorsTable.shopId] = shopId
                        table[categoryId] = containerId

                        table[name] = constructor.name
                        table[isInStock] = constructor.isInStock
                    }.value
                }
            }
        } catch (e: Exception) {
            throw DucksBadRequestError("Ошибка при добавлении базовых сущностей кофешопа")
        }
    }

    fun insertNewCategory(
        shopId: Long,
        request: CreateConstructorCategoryRequest,
    ): Long {
        return CoffeeConstructorCategoryTable.insertAndGetId { table ->
            table[CoffeeConstructorCategoryTable.shopId] = shopId
            table[name] = request.name
        }.value
    }

    fun insertNewConstructor(
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
        // 1. Выполняем запрос с LEFT JOIN для получения всех категорий + их конструкторов
        val results = CoffeeConstructorCategoryTable
            .join(
                CoffeeConstructorsTable,
                joinType = JoinType.LEFT,
                onColumn = CoffeeConstructorCategoryTable.id,
                otherColumn = CoffeeConstructorsTable.categoryId
            )
            .selectAll()
            .where { CoffeeConstructorCategoryTable.shopId eq shopId }
            .orderBy(CoffeeConstructorCategoryTable.name, SortOrder.ASC)
            .toList()

        // 2. Группируем результаты по категориям
        val categoriesWithConstructors = results
            .groupBy { row ->
                // Маппим категорию из результата (она всегда есть благодаря основной таблице)
                row.mapToConstructorCategoryDTO()
            }
            .map { (category, rows) ->
                // 3. Для каждой категории собираем список конструкторов (фильтруем null из-за LEFT JOIN)
                val constructors = rows
                    .map { row ->
                        // Конструктор может быть null если у категории нет конструкторов (LEFT JOIN)
                        row.mapToConstructorDTO()
                    }
                    .sortedBy { it.id } // Сортируем конструкторы внутри категории

                // 4. Формируем DTO для категории + её конструкторов
                SellerCoffeeCategoryDTO(
                    category = category,
                    constructors = constructors
                )
            }
            .sortedBy { it.category.id } // Сортируем категории

        // 5. Возвращаем финальный результат
        return SellerCoffeeCategoriesWithConstructorsDTO(
            data = categoriesWithConstructors
        )
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
            defaultConstructorNumber = null,
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
            defaultConstructorNumber = null,
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
            defaultConstructorNumber = 0,
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
        val defaultConstructorNumber: Int?,
        val constructors: List<Constructor>,
    )

    private data class Constructor(
        val name: String,
        val isInStock: Boolean,
    )
}
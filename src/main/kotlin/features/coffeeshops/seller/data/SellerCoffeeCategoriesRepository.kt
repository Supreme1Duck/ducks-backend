package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.database.CoffeeCategoryGroupTable
import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.mappers.mapToCategoryWithGroupDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryWithCountDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryWithGroupDTO
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeCategoriesRepository {

    /**
     * Плоский список категорий, у каждой — её группа. Порядок: сначала по группе,
     * внутри группы — по id категории, чтобы селлер видел стабильные разделы.
     */
    suspend fun getCategories(): List<CoffeeCategoryWithGroupDTO> {
        return newSuspendedTransaction {
            CoffeeProductCategoryTable
                .join(
                    otherTable = CoffeeCategoryGroupTable,
                    joinType = JoinType.INNER,
                    onColumn = CoffeeProductCategoryTable.groupId,
                    otherColumn = CoffeeCategoryGroupTable.id,
                )
                .selectAll()
                .orderBy(CoffeeCategoryGroupTable.sortOrder, SortOrder.ASC)
                .orderBy(CoffeeProductCategoryTable.id, SortOrder.ASC)
                .map { it.mapToCategoryWithGroupDTO() }
        }
    }

    suspend fun getCategoriesWithProductCount(shopId: Long): List<CoffeeCategoryWithCountDTO> {
        return newSuspendedTransaction {
            CoffeeProductCategoryTable
                .join(
                    otherTable = CoffeeProductTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeProductCategoryTable.id,
                    otherColumn = CoffeeProductTable.categoryId,
                    additionalConstraint = { CoffeeProductTable.shopId eq shopId }
                )
                .select(
                    CoffeeProductCategoryTable.id,
                    CoffeeProductCategoryTable.name,
                    CoffeeProductTable.id.count()
                )
                .groupBy(CoffeeProductCategoryTable.id, CoffeeProductCategoryTable.name)
                .map {
                    CoffeeCategoryWithCountDTO(
                        id = it[CoffeeProductCategoryTable.id].value,
                        name = it[CoffeeProductCategoryTable.name],
                        productCount = it[CoffeeProductTable.id.count()],
                    )
                }
        }
    }
}

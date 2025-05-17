package com.ducks.database.repository

import com.ducks.database.entity.ShopProductCategoryEntity
import com.ducks.dto.ShopProductCategoryDTO
import com.ducks.mapper.mapToModel
import com.ducks.model.ShopProductCategoryModel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ShopProductCategoryRepository {

    suspend fun getSuperCategories(): List<ShopProductCategoryModel> {
        return newSuspendedTransaction {
            val allCategories = ShopProductCategoryEntity.all().toList()
            val superCategories = getSuperCategories(allCategories)
                .map { it.mapToModel() }

            superCategories
        }
    }

    suspend fun getById(id: Long): ShopProductCategoryEntity {
        return ShopProductCategoryEntity[id]
    }

    suspend fun insertCategory(
        dto: ShopProductCategoryDTO
    ): ShopProductCategoryEntity? {
        return newSuspendedTransaction {
            try {
                val parentEntity = dto.parentCategoryId?.let {
                    ShopProductCategoryEntity.findById(it)
                }
                val superCategoryEntity = dto.superCategoryId?.let {
                    ShopProductCategoryEntity.findById(it)
                }

                ShopProductCategoryEntity.new {
                    name = dto.name
                    parent = parentEntity
                    description = dto.description
                    superCategory = superCategoryEntity
                }
            } catch (e: Exception) {
                println("Ошибка вставки ${dto.name}, ${e.printStackTrace()}")
                null
            }
        }
    }

    suspend fun insertSuperCategory(
        dto: ShopProductCategoryDTO
    ): ShopProductCategoryEntity? {
        return newSuspendedTransaction {
            try {
                ShopProductCategoryEntity.new {
                    name = dto.name
                    description = dto.description
                    isSuperCategory = true
                }
            } catch (e: Exception) {
                println("Ошибка вставки супер категории ${dto.name}, ${dto.description} ${e.printStackTrace()}")
                null
            }
        }
    }

    private fun getSuperCategories(categories: List<ShopProductCategoryEntity>): List<ShopProductCategoryEntity> {
        return categories.filter { it.isSuperCategory }
    }
}
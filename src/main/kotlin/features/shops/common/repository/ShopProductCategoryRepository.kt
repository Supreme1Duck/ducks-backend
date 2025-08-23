package com.ducks.features.shops.common.repository

import com.ducks.features.shops.database.entity.ShopProductCategoryEntity
import com.ducks.features.shops.common.dto.ShopProductCategoryDTO
import com.ducks.features.shops.common.mapper.mapToModel
import com.ducks.features.shops.common.model.ShopProductCategoryModel
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class ShopProductCategoryRepository {

    suspend fun getSuperCategories(): List<ShopProductCategoryModel> {
        return newSuspendedTransaction {
            val allCategories = ShopProductCategoryEntity.all().toList()
            val superCategories = getSuperCategories(allCategories)
                .map { it.mapToModel() }

            superCategories
        }
    }

    suspend fun getById(id: Long): ShopProductCategoryDTO {
        return newSuspendedTransaction {
            val entity = ShopProductCategoryEntity[id]

            ShopProductCategoryDTO(
                id = entity.id.value,
                name = entity.name,
                description = entity.description,
                isSuperCategory = entity.isSuperCategory,
                superCategoryId = entity.superCategory?.id?.value,
                parentCategoryId = entity.parent?.id?.value
            )
        }
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
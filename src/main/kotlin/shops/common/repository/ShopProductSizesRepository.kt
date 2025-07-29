package com.ducks.shops.common.repository

import com.ducks.shops.database.entity.ShopProductSizeEntity
import com.ducks.shops.common.dto.ShopProductChildSizeDTO
import com.ducks.shops.common.dto.ShopProductSizeDTO
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Этот репозиторий как и [ShopProductSizeEntity] и [ShopProductSizeTable] используется для создания/удаления/получения уникальных размеров.
 * Работа с конкретными размерами продукта реализована в [ShopProductsWithSizesRepository].
 */
class ShopProductSizesRepository {
    suspend fun insertSize(name: String, parent: ShopProductSizeEntity? = null): ShopProductSizeEntity {
        return newSuspendedTransaction {
            ShopProductSizeEntity.new {
                this.parent = parent
                this.name = name
            }
        }
    }

    suspend fun getAllSizes(): List<ShopProductSizeDTO> {
        return newSuspendedTransaction {
            ShopProductSizeEntity.all()
                .toList()
                .filter {
                    it.isParent
                }.map { parent ->
                    ShopProductSizeDTO(
                        id = parent.id.value,
                        name = parent.name,
                        children = parent.children.toList().map { child ->
                            ShopProductChildSizeDTO(
                                id = child.id.value,
                                parentId = parent.id.value,
                                name = child.name,
                            )
                        }
                    )
                }
        }
    }
}
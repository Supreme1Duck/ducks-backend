package com.ducks.shops.common.repository

import com.ducks.shops.common.data.ShopProductsDataSource
import com.ducks.shops.common.database.table.ShopProductTable
import com.ducks.shops.common.dto.SearchResultDTO
import com.ducks.shops.common.dto.ShopProductPreviewDTO
import com.ducks.shops.common.model.SeasonModel
import com.ducks.shops.common.model.ShopProductModel
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ShopProductsRepository(
    private val dataSource: ShopProductsDataSource,
) {

    suspend fun insertProduct(
        shopId: Long,
        categoryId: Long,
        seasonModel: SeasonModel? = null,
        shopProductModel: ShopProductModel,
    ): Long {
        return newSuspendedTransaction {
            ShopProductTable.insertAndGetId { table ->
                table[name] = shopProductModel.name
                table[description] = shopProductModel.description
                table[brandName] = shopProductModel.brandName
                table[price] = shopProductModel.price
                table[imageUrls] = shopProductModel.imageUrls
                table[shop] = shopId
                table[category] = categoryId
                seasonModel?.let {
                    table[seasonId] = it.id
                }
            }.value
        }
    }

    suspend fun updateProduct(
        shopId: Long,
        categoryId: Long,
        seasonModel: SeasonModel? = null,
        shopProductModel: ShopProductModel,
    ) {
        return newSuspendedTransaction {
            ShopProductTable.update(
                body = {table ->
                    table[name] = shopProductModel.name
                    table[description] = shopProductModel.description
                    table[brandName] = shopProductModel.brandName
                    table[price] = shopProductModel.price
                    table[imageUrls] = shopProductModel.imageUrls
                    table[shop] = shopId
                    table[category] = categoryId
                    seasonModel?.let {
                        table[seasonId] = it.id
                    }
                }
            )
        }
    }

    suspend fun searchWithQueryOrFilters(
        query: String?,
        shopId: Long?,
        categoryIds: List<Long>?,
        colorIds: List<Int>?,
        seasonIds: List<Int>?,
        sizeIds: List<Long>?,
        lastId: Long?,
        limit: Int?,
    ): SearchResultDTO {
        return newSuspendedTransaction {
            val products = dataSource.searchWithQuery(
                query = query,
                shopId = shopId,
                categoriesIds = categoryIds,
                colorIds = colorIds,
                seasonModelIds = seasonIds,
                sizeIds = sizeIds,
                lastId = lastId,
                limit = limit
            )

            val filters = dataSource.requestAvailableFiltersFromDB(
                productIds = products.map { it.id },
                shopId = shopId,
            )

            SearchResultDTO(
                products = products,
                filters = filters
            )
        }
    }

    suspend fun getAllProducts(
        lastId: Long? = null,
        limit: Int? = null,
    ): SearchResultDTO {
        return newSuspendedTransaction {
            val list = dataSource.requestProductsFromDB(shopId = null, lastId = lastId, limit = limit)
            val filters = dataSource.requestAvailableFiltersFromDB(shopId = null, productIds = null)

            SearchResultDTO(
                products = list,
                filters = filters
            )
        }
    }

    suspend fun getProductsByShop(
        shopId: Long,
        lastId: Long? = null,
        limit: Int? = null,
    ): SearchResultDTO {
        return newSuspendedTransaction {
            val productsList = dataSource.requestProductsFromDB(shopId = shopId, lastId = lastId, limit = limit)
            val filters = dataSource.requestAvailableFiltersFromDB(shopId = shopId, productIds = null)

            SearchResultDTO(
                products = productsList,
                filters = filters
            )
        }
    }

    suspend fun getShopProductById(
        id: Long
    ) {
        return newSuspendedTransaction {
            val availableSizes = dataSource.requestSizesByProduct(id)
            dataSource.getShopProduct(id, availableSizes)
        }
    }

    suspend fun getProductsByShopPreview(
        shopId: Long,
        lastId: Long?,
        limit: Int?,
    ): List<ShopProductPreviewDTO> {
        return newSuspendedTransaction {
            dataSource.getProductsPreviewByShop(shopId, lastId, limit)
        }
    }
}
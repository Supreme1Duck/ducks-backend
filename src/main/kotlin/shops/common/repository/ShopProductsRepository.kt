package com.ducks.shops.common.repository

import com.ducks.common.data.UpdateMap
import com.ducks.shops.common.data.ShopProductsDataSource
import com.ducks.shops.database.table.ShopProductTable
import com.ducks.shops.common.dto.SearchResultDTO
import com.ducks.shops.common.dto.ShopProductDTO
import com.ducks.shops.common.dto.ShopProductPreviewDTO
import com.ducks.shops.common.model.SeasonModel
import com.ducks.shops.common.model.ShopProductModel
import com.ducks.shops.seller.domain.ShopImageRepository
import com.ducks.shops.seller.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal

class ShopProductsRepository(
    private val dataSource: ShopProductsDataSource,
    private val shopImageRepository: ShopImageRepository,
) {

    fun insertProduct(
        shopId: Long,
        categoryId: Long,
        seasonModel: SeasonModel? = null,
        shopProductModel: ShopProductModel,
    ): Long {
        return ShopProductTable.insertAndGetId { table ->
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

    fun updateProduct(
        shopId: Long,
        productId: Long,
        updateMap: UpdateMap,
    ) {
        ShopProductTable.update(where = {
            (ShopProductTable.shop eq shopId) and (ShopProductTable.id eq productId)
        }) { table ->
            updateMap.forEach {
                when (it.key) {
                    SELLER_PRODUCT_MAP_NAME_KEY -> {
                        table[name] = Json.decodeFromJsonElement<String>(it.value)
                    }

                    SELLER_PRODUCT_MAP_DESCRIPTION_KEY -> {
                        table[description] = Json.decodeFromJsonElement<String?>(it.value)
                    }

                    SELLER_PRODUCT_MAP_BRANDNAME_KEY -> {
                        table[brandName] = Json.decodeFromJsonElement<String?>(it.value)
                    }

                    SELLER_PRODUCT_MAP_PRICE_KEY -> {
                        table[price] = Json.decodeFromJsonElement<BigDecimal?>(it.value)
                    }

                    SELLER_PRODUCT_MAP_IMAGES_KEY -> {
                        val imageUrls = Json.decodeFromJsonElement<List<String>>(it.value)
                        deleteUnusedImages(shopId = productId, productId = shopId, imageUrls = imageUrls)
                        table[ShopProductTable.imageUrls] = imageUrls
                    }

                    SELLER_PRODUCT_MAP_CATEGORY_KEY -> {
                        table[category] = Json.decodeFromJsonElement<Long>(it.value)
                    }

                    SELLER_PRODUCT_MAP_SEASON_KEY -> {
                        table[seasonId] = Json.decodeFromJsonElement<Int?>(it.value)
                    }
                }
            }
        }
    }

    private fun deleteUnusedImages(shopId: Long, productId: Long, imageUrls: List<String>): List<String> {
        val existingImageUrls = ShopProductTable
            .select(ShopProductTable.imageUrls)
            .where { ShopProductTable.id eq productId }
            .map { it[ShopProductTable.imageUrls] }
            .first()

        val listToDelete = existingImageUrls.mapNotNull {
            if (imageUrls.contains(it)) {
                null
            } else {
                it
            }
        }

        listToDelete.forEach {
            shopImageRepository.deleteImage(shopId, it)
        }

        return imageUrls
    }

    fun deleteProduct(
        shopId: Long,
        productId: Long,
    ) {
        ShopProductTable.deleteWhere { table ->
            (shop eq shopId) and (id eq productId)
        }
    }

    fun searchWithQueryOrFilters(
        query: String?,
        shopId: Long?,
        categoryIds: List<Long>?,
        colorIds: List<Int>?,
        seasonIds: List<Int>?,
        sizeIds: List<Long>?,
        lastId: Long?,
        limit: Int?,
    ): SearchResultDTO {
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

        return SearchResultDTO(
            products = products,
            filters = filters
        )
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
    ): ShopProductDTO {
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
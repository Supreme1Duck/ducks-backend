package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.seller.routings.request.products.CoffeeProductSizeRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.common.data.UpdateMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal

class SellerCoffeeProductDataSource {

    suspend fun insertProduct(shopId: Long, productRequest: CreateCoffeeProductRequest) {
        newSuspendedTransaction {
            val pricesStartsFrom: BigDecimal = productRequest.sizes.minOf {
                it.price ?: 0.toBigDecimal()
            }.takeIf { it != BigDecimal.ZERO } ?: throw IllegalArgumentException("Минимальная цена не может быть равна 0")

            CoffeeProductTable.insert { table ->
                table[name] = productRequest.name
                table[description] = productRequest.description
                table[priceFrom] = pricesStartsFrom
                table[categoryId] = productRequest.categoryId
                table[CoffeeProductTable.shopId] = shopId

                table[sizes] = productRequest.sizes.map {
                    CoffeeProductSizeDTO(
                        sizeName = it.sizeName,
                        sizeValue = it.sizeValue,
                        price = it.price
                    )
                }

                table[imageUrl] = productRequest.imageUrl

                table[carbohydrates] = productRequest.carbohydrates
                table[protein] = productRequest.protein
                table[fats] = productRequest.fats
                table[calories] = productRequest.calories
            }
        }
    }

    suspend fun updateProduct(
        shopId: Long,
        productId: Long,
        updateMap: UpdateMap,
    ) {
        newSuspendedTransaction {
            if (updateMap.isEmpty()) {
                throw IllegalStateException()
            }

            CoffeeProductTable.update(
                where = {
                    (CoffeeProductTable.id eq productId) and (CoffeeProductTable.shopId eq shopId)
                }
            ) { table ->
                updateMap.forEach {
                    when (it.key) {
                        UPDATE_MAP_COFFEE_PRODUCT_NAME -> {
                            table[name] = Json.decodeFromJsonElement<String>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_DESCRIPTION -> {
                            table[description] = Json.decodeFromJsonElement<String>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_CATEGORY -> {
                            table[categoryId] = Json.decodeFromJsonElement<Long>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_SIZE -> {
                            table[sizes] = Json.decodeFromJsonElement<List<CoffeeProductSizeRequest>>(it.value)
                                .map { size ->
                                    CoffeeProductSizeDTO(
                                        sizeName = size.sizeName,
                                        sizeValue = size.sizeValue,
                                        price = size.price,
                                    )
                                }
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_IMAGE -> {
                            table[imageUrl] = Json.decodeFromJsonElement<String>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_CARBOHYDRATES -> {
                            table[carbohydrates] = Json.decodeFromJsonElement<String?>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_PROTEIN -> {
                            table[protein] = Json.decodeFromJsonElement<String?>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_FATS -> {
                            table[fats] = Json.decodeFromJsonElement<String?>(it.value)
                        }

                        UPDATE_MAP_COFFEE_PRODUCT_CALORIES -> {
                            table[calories] = Json.decodeFromJsonElement<String?>(it.value)
                        }
                    }
                }
            }
        }
    }

    suspend fun deleteProduct(shopId: Long, productId: Long) {
        newSuspendedTransaction {
            CoffeeProductTable.deleteWhere {
                (CoffeeProductTable.shopId) eq shopId and (CoffeeProductTable.id eq productId)
            }
        }
    }
}
package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductRequest
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal

class SellerCoffeeProductDataSource {

    suspend fun getProductDetails(productId: Long): CoffeeProductWithDetailsDTO {
        return newSuspendedTransaction {
            CoffeeProductTable
                .join(
                    CoffeeProductsWithConstructorsTable,
                    joinType = JoinType.LEFT,
                    CoffeeProductsWithConstructorsTable.product,
                    CoffeeProductTable.id,
                )
                .join(
                    CoffeeConstructorsTable,
                    joinType = JoinType.LEFT,
                    CoffeeConstructorsTable.id,
                    CoffeeProductsWithConstructorsTable.constructor
                )
                .join(
                    CoffeeModifiedConstructorCategoryTable,
                    joinType = JoinType.LEFT,
                    CoffeeProductsWithConstructorsTable.modifiedCategory,
                    CoffeeModifiedConstructorCategoryTable.id,
                )
                .join(
                    CoffeeConstructorCategoryTable,
                    joinType = JoinType.LEFT,
                    CoffeeModifiedConstructorCategoryTable.categoryId,
                    CoffeeConstructorCategoryTable.id
                )
                .join(
                    CoffeeProductCategoryTable,
                    joinType = JoinType.LEFT,
                    CoffeeProductTable.categoryId,
                    CoffeeProductCategoryTable.id
                )
                .selectAll()
                .where {
                    CoffeeProductTable.id eq productId
                }
                .toList()
                .mapToCoffeeProductWithDetailsDTO()
        }
    }

    suspend fun insertProduct(
        shopId: Long,
        productRequest: CreateCoffeeProductRequest
    ) {
        newSuspendedTransaction {
            val pricesStartsFrom: BigDecimal = productRequest.sizes.minOf {
                it.price
            }.takeIf { it != BigDecimal.ZERO }
                ?: throw IllegalArgumentException("Минимальная цена не может быть равна 0")

            val productId = CoffeeProductTable.insertAndGetId { table ->
                table[name] = productRequest.name
                table[description] = productRequest.description
                table[priceFrom] = pricesStartsFrom
                table[categoryId] = productRequest.categoryId
                table[CoffeeProductTable.shopId] = shopId

                table[sizes] = productRequest.sizes.map { coffeeProductSizeRequest ->
                    CoffeeProductSizeDTO(
                        id = coffeeProductSizeRequest.id,
                        sizeName = coffeeProductSizeRequest.sizeName,
                        sizeValue = coffeeProductSizeRequest.sizeValue,
                        price = coffeeProductSizeRequest.price,
                    )
                }

                table[imageUrl] = productRequest.imageUrl
                table[minutesToCook] = productRequest.minutesToCook

                table[carbohydrates] = productRequest.carbohydrates
                table[protein] = productRequest.protein
                table[fats] = productRequest.fats
                table[calories] = productRequest.calories
            }

            productRequest.constructors?.let { constructors ->
                constructors.forEach { (categoryRequest, constructors) ->
                    // Вставляем или получаем существующую категорию
                    val categoryId = CoffeeConstructorCategoryTable
                        .select(CoffeeConstructorCategoryTable.id)
                        .where {
                            CoffeeConstructorCategoryTable.id eq categoryRequest.id
                        }
                        .map {
                            it[CoffeeConstructorCategoryTable.id].value
                        }
                        .first()

                    val modifiedCategoryId = CoffeeModifiedConstructorCategoryTable.insertAndGetId { table ->
                        table[CoffeeModifiedConstructorCategoryTable.categoryId] = categoryId
                        table[defaultConstructorIds] = categoryRequest.defaultConstructorIds
                        table[maxSelection] = categoryRequest.maxSelection
                        table[minSelection] = categoryRequest.minSelection
                    }

                    // Вставляем конструкторы для этой категории
                    constructors.forEach { constructorRequest ->
                        // Вставляем связь продукт-конструктор-модифицированная категория
                        CoffeeProductsWithConstructorsTable.insert {
                            it[constructor] = constructorRequest.id
                            it[modifiedCategory] = modifiedCategoryId
                            it[product] = productId
                        }
                    }
                }
            }
        }
    }

    suspend fun updateProduct(
        shopId: Long,
        productRequest: UpdateCoffeeProductRequest,
    ) {
        newSuspendedTransaction {
            val pricesStartsFrom: BigDecimal = productRequest.sizes.minOf {
                it.price
            }.takeIf { it != BigDecimal.ZERO }
                ?: throw IllegalArgumentException("Минимальная цена не может быть равна 0")

            CoffeeProductTable.update({
                (CoffeeProductTable.id eq productRequest.productId) and (CoffeeProductTable.shopId eq shopId)
            }) { table ->
                table[name] = productRequest.name
                table[description] = productRequest.description
                table[priceFrom] = pricesStartsFrom
                table[categoryId] = productRequest.categoryId
                table[imageUrl] = productRequest.imageUrl
                table[minutesToCook] = productRequest.minutesToCook
                table[carbohydrates] = productRequest.carbohydrates
                table[protein] = productRequest.protein
                table[fats] = productRequest.fats
                table[calories] = productRequest.calories
                table[sizes] = productRequest.sizes.map { sizeRequest ->
                    CoffeeProductSizeDTO(
                        id = sizeRequest.id,
                        sizeName = sizeRequest.sizeName,
                        sizeValue = sizeRequest.sizeValue,
                        price = sizeRequest.price,
                    )
                }
            }

            // Удаляем старые конструкторы
            val oldLinks = CoffeeProductsWithConstructorsTable
                .select(CoffeeProductsWithConstructorsTable.modifiedCategory)
                .where { CoffeeProductsWithConstructorsTable.product eq productRequest.productId }
                .map { it[CoffeeProductsWithConstructorsTable.modifiedCategory].value }

            CoffeeProductsWithConstructorsTable.deleteWhere {
                CoffeeProductsWithConstructorsTable.product eq productRequest.productId
            }

            oldLinks.forEach { modifiedCategoryId ->
                CoffeeModifiedConstructorCategoryTable.deleteWhere {
                    CoffeeModifiedConstructorCategoryTable.id eq modifiedCategoryId
                }
            }

            // Вставляем новые конструкторы
            productRequest.constructors?.let { constructors ->
                constructors.forEach { constructorRequest ->
                    val categoryRequest = constructorRequest.category

                    val categoryId = CoffeeConstructorCategoryTable
                        .select(CoffeeConstructorCategoryTable.id)
                        .where { CoffeeConstructorCategoryTable.id eq categoryRequest.id }
                        .map { it[CoffeeConstructorCategoryTable.id].value }
                        .first()

                    val modifiedCategoryId = CoffeeModifiedConstructorCategoryTable.insertAndGetId { table ->
                        table[CoffeeModifiedConstructorCategoryTable.categoryId] = categoryId
                        table[defaultConstructorIds] = categoryRequest.defaultConstructorIds
                        table[maxSelection] = categoryRequest.maxSelection
                        table[minSelection] = categoryRequest.minSelection
                    }

                    constructorRequest.constructors.forEach { item ->
                        CoffeeProductsWithConstructorsTable.insert {
                            it[constructor] = item.id
                            it[modifiedCategory] = modifiedCategoryId
                            it[product] = productRequest.productId
                        }
                    }
                }
            }
        }
    }

    suspend fun deleteProduct(shopId: Long, productId: Long) {
        newSuspendedTransaction {
            CoffeeProductTable.deleteWhere {
                (CoffeeProductTable.shopId eq shopId) and (CoffeeProductTable.id eq productId)
            }
        }
    }
}
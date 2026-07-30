package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductRequest
import com.ducks.util.DucksBadRequestError
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
    ): Long {
        return newSuspendedTransaction {
            checkNoDuplicatedConstructors(
                productRequest.constructors.orEmpty().flatMap { category ->
                    category.constructors.map { RequestedConstructor(id = it.id, name = it.name) }
                }
            )

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
                table[inStock] = productRequest.isInStock

                table[carbohydrates] = productRequest.carbohydrates
                table[protein] = productRequest.protein
                table[fats] = productRequest.fats
                table[calories] = calculateCalories(
                    protein = productRequest.protein,
                    fats = productRequest.fats,
                    carbohydrates = productRequest.carbohydrates,
                )
            }

            productRequest.constructors?.let { constructors ->
                constructors.forEach { (categoryRequest, constructors) ->
                    // Вставляем или получаем существующую категорию
                    val categoryId = requireConstructorCategoryId(
                        shopId = shopId,
                        categoryId = categoryRequest.id,
                    )

                    val modifiedCategoryId = CoffeeModifiedConstructorCategoryTable.insertAndGetId { table ->
                        table[CoffeeModifiedConstructorCategoryTable.categoryId] = categoryId
                        table[defaultConstructorIds] = categoryRequest.defaultConstructorIds
                        table[maxSelection] = categoryRequest.maxSelection
                        table[minSelection] = categoryRequest.minSelection
                    }

                    // Вставляем конструкторы для этой категории, попутно заводя новые
                    val constructorIds = resolveConstructorIds(
                        shopId = shopId,
                        categoryId = categoryId,
                        requested = constructors.map { RequestedConstructor(id = it.id, name = it.name) },
                    )

                    constructorIds.forEach { constructorId ->
                        // Вставляем связь продукт-конструктор-модифицированная категория
                        CoffeeProductsWithConstructorsTable.insert {
                            it[constructor] = constructorId
                            it[modifiedCategory] = modifiedCategoryId
                            it[product] = productId
                        }
                    }
                }
            }

            productId.value
        }
    }

    suspend fun updateProduct(
        shopId: Long,
        productRequest: UpdateCoffeeProductRequest,
    ) {
        newSuspendedTransaction {
            checkNoDuplicatedConstructors(
                productRequest.constructors.orEmpty().flatMap { category ->
                    category.constructors.map { RequestedConstructor(id = it.id, name = it.name) }
                }
            )

            val pricesStartsFrom: BigDecimal = productRequest.sizes.minOf {
                it.price
            }.takeIf { it != BigDecimal.ZERO }
                ?: throw IllegalArgumentException("Минимальная цена не может быть равна 0")

            val updatedRows = CoffeeProductTable.update({
                (CoffeeProductTable.id eq productRequest.productId) and (CoffeeProductTable.shopId eq shopId)
            }) { table ->
                table[name] = productRequest.name
                table[description] = productRequest.description
                table[priceFrom] = pricesStartsFrom
                table[categoryId] = productRequest.categoryId
                table[imageUrl] = productRequest.imageUrl
                table[minutesToCook] = productRequest.minutesToCook
                table[inStock] = productRequest.isInStock
                table[carbohydrates] = productRequest.carbohydrates
                table[protein] = productRequest.protein
                table[fats] = productRequest.fats
                table[calories] = calculateCalories(
                    protein = productRequest.protein,
                    fats = productRequest.fats,
                    carbohydrates = productRequest.carbohydrates,
                )
                table[sizes] = productRequest.sizes.map { sizeRequest ->
                    CoffeeProductSizeDTO(
                        id = sizeRequest.id,
                        sizeName = sizeRequest.sizeName,
                        sizeValue = sizeRequest.sizeValue,
                        price = sizeRequest.price,
                    )
                }
            }

            // Продукт чужого магазина не обновится, но связи с конструкторами ниже
            // переписываются по одному productId — без этой проверки продавец мог
            // перекроить состав чужого продукта.
            if (updatedRows == 0) {
                throw DucksBadRequestError("Продукт ${productRequest.productId} не найден")
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

                    val categoryId = requireConstructorCategoryId(
                        shopId = shopId,
                        categoryId = categoryRequest.id,
                    )

                    val modifiedCategoryId = CoffeeModifiedConstructorCategoryTable.insertAndGetId { table ->
                        table[CoffeeModifiedConstructorCategoryTable.categoryId] = categoryId
                        table[defaultConstructorIds] = categoryRequest.defaultConstructorIds
                        table[maxSelection] = categoryRequest.maxSelection
                        table[minSelection] = categoryRequest.minSelection
                    }

                    val constructorIds = resolveConstructorIds(
                        shopId = shopId,
                        categoryId = categoryId,
                        requested = constructorRequest.constructors.map { item ->
                            RequestedConstructor(id = item.id, name = item.name)
                        },
                    )

                    constructorIds.forEach { constructorId ->
                        CoffeeProductsWithConstructorsTable.insert {
                            it[constructor] = constructorId
                            it[modifiedCategory] = modifiedCategoryId
                            it[product] = productRequest.productId
                        }
                    }
                }
            }
        }
    }

    /**
     * Один конструктор нельзя положить в две категории одного продукта — это те же самые
     * добавки, продублированные в меню. На уровне бд это запрещено уникальным индексом
     * (product, constructor), здесь отдаём понятную ошибку до записи.
     */
    private fun checkNoDuplicatedConstructors(requested: List<RequestedConstructor>) {
        val duplicatedIds = requested
            .groupingBy { it.id }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .toList()

        if (duplicatedIds.isEmpty()) return

        val namesById = CoffeeConstructorsTable
            .select(CoffeeConstructorsTable.id, CoffeeConstructorsTable.name)
            .where {
                CoffeeConstructorsTable.id inList duplicatedIds
            }
            .associate {
                it[CoffeeConstructorsTable.id].value to it[CoffeeConstructorsTable.name]
            }

        // У ещё не сохранённой добавки названия в бд нет — берём то, что прислали.
        val duplicatedNames = duplicatedIds.map { id ->
            namesById[id]
                ?: requested.first { it.id == id }.name
                ?: "id $id"
        }

        throw DucksBadRequestError(
            "Конструктор можно добавить к продукту только один раз, " +
                    "уберите повторы: ${duplicatedNames.joinToString()}"
        )
    }

    /**
     * Отдаёт id конструкторов, которые можно класть в связь с продуктом.
     *
     * Приложение помечает ещё не сохранённую добавку отрицательным временным id
     * (см. SellerCoffeeConstructorsDataSource.saveConstructors) — такую заводим здесь же,
     * в той категории, в которой её прислали. Раньше временный id уходил прямо в FK-колонку,
     * и postgres валил запрос ошибкой внешнего ключа.
     *
     * Остальные id обязаны существовать и принадлежать этому же магазину: без проверки на
     * shopId валидный id чужого магазина привязывался к продукту без единого вопроса.
     */
    private fun resolveConstructorIds(
        shopId: Long,
        categoryId: Long,
        requested: List<RequestedConstructor>,
    ): List<Long> {
        if (requested.isEmpty()) return emptyList()

        val existingIds = CoffeeConstructorsTable
            .select(CoffeeConstructorsTable.id)
            .where {
                (CoffeeConstructorsTable.id inList requested.map { it.id }) and
                        (CoffeeConstructorsTable.shopId eq shopId)
            }
            .map { it[CoffeeConstructorsTable.id].value }
            .toSet()

        return requested.map { item ->
            when {
                item.id < 0 -> insertRequestedConstructor(
                    shopId = shopId,
                    categoryId = categoryId,
                    item = item,
                )

                item.id in existingIds -> item.id

                else -> throw DucksBadRequestError("Добавка ${item.id} не найдена")
            }
        }
    }

    /**
     * Цена и наличие в запросе продукта не приходят: новая добавка заводится бесплатной
     * и в наличии, продавец правит их на экране конструкторов.
     */
    private fun insertRequestedConstructor(
        shopId: Long,
        categoryId: Long,
        item: RequestedConstructor,
    ): Long {
        val constructorName = item.name?.takeIf { it.isNotBlank() }
            ?: throw DucksBadRequestError("У новой добавки не пришло название")

        return CoffeeConstructorsTable.insertAndGetId { table ->
            table[CoffeeConstructorsTable.shopId] = shopId
            table[CoffeeConstructorsTable.categoryId] = categoryId
            table[CoffeeConstructorsTable.name] = constructorName
            table[CoffeeConstructorsTable.price] = BigDecimal.ZERO
            table[CoffeeConstructorsTable.isInStock] = true
        }.value
    }

    /** Общий вид добавки из запросов на создание и на обновление продукта. */
    private data class RequestedConstructor(
        val id: Long,
        val name: String?,
    )

    /**
     * Категория добавок тоже проверяется на принадлежность магазину: раньше здесь был
     * `.first()` без фильтра по shopId — на несуществующей категории он падал
     * NoSuchElementException'ом в 500, а на чужой молча срабатывал.
     */
    private fun requireConstructorCategoryId(shopId: Long, categoryId: Long): Long {
        return CoffeeConstructorCategoryTable
            .select(CoffeeConstructorCategoryTable.id)
            .where {
                (CoffeeConstructorCategoryTable.id eq categoryId) and
                        (CoffeeConstructorCategoryTable.shopId eq shopId)
            }
            .map { it[CoffeeConstructorCategoryTable.id].value }
            .firstOrNull()
            ?: throw DucksBadRequestError("Категория добавок $categoryId не найдена")
    }

    suspend fun deleteProduct(shopId: Long, productId: Long) {
        newSuspendedTransaction {
            CoffeeProductTable.deleteWhere {
                (CoffeeProductTable.shopId eq shopId) and (CoffeeProductTable.id eq productId)
            }
        }
    }

    suspend fun updateStock(shopId: Long, productId: Long, inStock: Boolean) {
        newSuspendedTransaction {
            CoffeeProductTable.update({
                (CoffeeProductTable.id eq productId) and (CoffeeProductTable.shopId eq shopId)
            }) { table ->
                table[CoffeeProductTable.inStock] = inStock
            }
        }
    }

    /**
     * Калорийность считается по формуле Атуотера (ккал на 100г продукта):
     * белки * 4 + углеводы * 4 + жиры * 9.
     * Если хотя бы один из компонентов не указан, калорийность не считается.
     */
    private fun calculateCalories(protein: Int?, fats: Int?, carbohydrates: Int?): Int? {
        if (protein == null || fats == null || carbohydrates == null) return null

        return protein * 4 + carbohydrates * 4 + fats * 9
    }
}
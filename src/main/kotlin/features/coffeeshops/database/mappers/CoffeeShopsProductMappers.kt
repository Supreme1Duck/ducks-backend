package com.ducks.features.coffeeshops.database.mappers

import com.ducks.features.coffeeshops.client.data.model.dto.*
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import com.ducks.features.coffeeshops.database.CoffeeConstructorCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.database.CoffeeModifiedConstructorCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import org.jetbrains.exposed.v1.core.ResultRow

fun List<ResultRow>.mapToCoffeeProductWithDetailsDTO(): CoffeeProductWithDetailsDTO {
    // Сначала получаем основные данные продукта (из первой строки)
    val firstRow = this.firstOrNull()
        ?: throw NoSuchElementException("Product with id not found")

    val sizes = firstRow[CoffeeProductTable.sizes]
    val minPrice = sizes.minOf {
        it.price
    }
    val minSize = sizes.first().sizeValue

    val product = CoffeeProductWithDetailsDTO(
        id = firstRow[CoffeeProductTable.id].value,
        name = firstRow[CoffeeProductTable.name],
        description = firstRow[CoffeeProductTable.description],
        categoryId = firstRow[CoffeeProductTable.categoryId].value,
        shopId = firstRow[CoffeeProductTable.shopId].value,
        imageUrl = firstRow[CoffeeProductTable.imageUrl],
        minutesToCook = firstRow[CoffeeProductTable.minutesToCook] ?: 0,
        inStock = firstRow[CoffeeProductTable.inStock],
        minPrice = minPrice,
        minSize = "от $minSize",
        sizes = sizes,
        nutrients = NutrientsDTO(
            calories = firstRow[CoffeeProductTable.calories],
            carbohydrates = firstRow[CoffeeProductTable.carbohydrates],
            protein = firstRow[CoffeeProductTable.protein],
            fats = firstRow[CoffeeProductTable.fats],
        ),
        constructors = emptyList() // временно пусто
    )

    // Собираем конструкторы по категориям
    val constructorsByCategory: List<CoffeeConstructorsDTO> =
        this
            // 1. Фильтруем строки без конструкторов (защита от NULL при LEFT JOIN)
            // Suppress работает корректно
            .filter { row -> row[CoffeeConstructorsTable.id] != null }
            // 2. Группируем по категориям
            .groupBy { row ->
                CoffeeConstructorCategoryDTO(
                    id = row[CoffeeConstructorCategoryTable.id].value,
                    name = row[CoffeeConstructorCategoryTable.name],
                    defaultConstructorIds = row[CoffeeModifiedConstructorCategoryTable.defaultConstructorIds],
                    maxSelection = row[CoffeeModifiedConstructorCategoryTable.maxSelection],
                    minSelection = row[CoffeeModifiedConstructorCategoryTable.minSelection],
                )
            }
            // 4. Преобразуем мапу в список ConstructorsDTO
            .map { (category, rows) ->
                CoffeeConstructorsDTO(
                    category = category,
                    constructors = rows.map { row ->
                        CoffeeConstructorDTO(
                            id = row[CoffeeConstructorsTable.id].value,
                            name = row[CoffeeConstructorsTable.name],
                            price = row[CoffeeConstructorsTable.price],
                            categoryId = row[CoffeeConstructorsTable.categoryId].value,
                            isInStock = row[CoffeeConstructorsTable.isInStock]
                        )
                    }
                )
            }
            .toList()

    return product.copy(
        constructors = constructorsByCategory,
    )
}

fun List<ResultRow>.mapToProductPreviewDTO(): CoffeeShopProductPreviewDTO {
    val firstRow = this.first()

    val constructors = this
        .filter { it[CoffeeConstructorsTable.id] != null }
        .groupBy { row ->
            CoffeeConstructorCategoryDTO(
                id = row[CoffeeConstructorCategoryTable.id].value,
                name = row[CoffeeConstructorCategoryTable.name],
                defaultConstructorIds = row[CoffeeModifiedConstructorCategoryTable.defaultConstructorIds],
                maxSelection = row[CoffeeModifiedConstructorCategoryTable.maxSelection],
                minSelection = row[CoffeeModifiedConstructorCategoryTable.minSelection],
            )
        }
        .map { (category, rows) ->
            CoffeeConstructorsDTO(
                category = category,
                constructors = rows.map { row ->
                    CoffeeConstructorDTO(
                        id = row[CoffeeConstructorsTable.id].value,
                        name = row[CoffeeConstructorsTable.name],
                        price = row[CoffeeConstructorsTable.price],
                        categoryId = row[CoffeeConstructorsTable.categoryId].value,
                        isInStock = row[CoffeeConstructorsTable.isInStock],
                    )
                }
            )
        }

    return CoffeeShopProductPreviewDTO(
        id = firstRow[CoffeeProductTable.id].value,
        name = firstRow[CoffeeProductTable.name],
        imageUrl = firstRow[CoffeeProductTable.imageUrl],
        categoryId = firstRow[CoffeeProductTable.categoryId].value,
        categoryName = firstRow[CoffeeProductCategoryTable.name],
        inStock = firstRow[CoffeeProductTable.inStock],
        minutesToCook = firstRow[CoffeeProductTable.minutesToCook],
        shopId = firstRow[CoffeeProductTable.shopId].value,
        shopName = firstRow[CoffeeShopTable.name],
        sizes = firstRow[CoffeeProductTable.sizes],
        constructors = constructors,
        description = firstRow[CoffeeProductTable.description],
        nutrients = NutrientsDTO(
            calories = firstRow[CoffeeProductTable.calories],
            carbohydrates = firstRow[CoffeeProductTable.carbohydrates],
            protein = firstRow[CoffeeProductTable.protein],
            fats = firstRow[CoffeeProductTable.fats],
        ),
    )
}
package com.ducks.database

import com.ducks.domain.InsertShopProductUseCase
import com.ducks.domain.SearchProductsUseCase
import com.ducks.model.ShopModel
import com.ducks.model.ShopProductModel
import com.ducks.repository.ShopProductCategoryRepository
import com.ducks.repository.ShopProductSizesRepository
import com.ducks.repository.ShopsRepository
import database.DatabaseFactory
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Application.installDB() {
    DatabaseFactory.init(environment.config.property("ktor.database.password").getString())
    testFunction()
}

// TODO Убрать все нижние функции
private fun Application.testFunction() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    coroutineScope.launch {
        searchSomeProducts()
    }
}

private suspend fun Application.searchSomeProducts() {
    val searchProductsUseCase by inject<SearchProductsUseCase>()

    val result = searchProductsUseCase.invoke(null, null, null, null, null)

    println("Продукты")
    result.products.forEach {
        println()
        println(it.name + " " + it.description)
    }

    println("Размеры")
    result.sizes.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }

    println("Цвета")
    result.colors.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }

    println("Категории")
    result.categories.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }

    println("Сезоны")
    result.seasons.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }
}

private suspend fun Application.deleteAllAndInsertNewShops() {
    val shopsRepository by inject<ShopsRepository>()
    val shopProductUseCase by inject<InsertShopProductUseCase>()

    shopsRepository.deleteAll()

    val shop = ShopModel(
        name = "Пижон",
        address = "Ленинская 122"
    )
    val shopId = shopsRepository.insert(shop).id

    val shopProducts = listOf(
        ShopProductModel(
            name = "Брюки",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            sizeIds = listOf(2, 3, 4, 5, 6, 7),
            shopId = shopId.value,
            categoryId = 7,
            price = null,
            seasonModel = null,
            colorId = null,
        ),
        ShopProductModel(
            name = "Майка",
            description = "Обычная",
            imageUrls = emptyList(),
            brandName = "Zara",
            price = null,
            shopId = shopId.value,
            categoryId = 7,
            sizeIds = listOf(30, 31, 32, 33, 34, 35, 36),
            seasonModel = null,
            colorId = null
        ),
        ShopProductModel(
            name = "Кроссовки",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            sizeIds = listOf(42, 43, 44, 45, 46, 47, 48),
            shopId = shopId.value,
            categoryId = 7,
            price = null,
            seasonModel = null,
            colorId = null
        ),
        ShopProductModel(
            name = "Кофта",
            description = "Обычная",
            imageUrls = emptyList(),
            brandName = "Zara",
            sizeIds = listOf(29, 30 ,31 ,32, 33, 34, 35),
            shopId = shopId.value,
            categoryId = 6,
            price = null,
            seasonModel = null,
            colorId = null
        ),
        ShopProductModel(
            name = "Туника",
            description = "Обычная",
            imageUrls = emptyList(),
            brandName = "Zara",
            sizeIds = listOf(29, 30 ,31 ,32, 33, 34, 35),
            shopId = shopId.value,
            categoryId = 5,
            price = null,
            seasonModel = null,
            colorId = null
        ),
        ShopProductModel(
            name = "Брюки",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            sizeIds = listOf(2, 3, 4, 5, 6, 7),
            shopId = shopId.value,
            categoryId = 4,
            price = null,
            seasonModel = null,
            colorId = null
        ),
        ShopProductModel(
            name = "Штаны",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            sizeIds = listOf(2, 3, 4, 5, 6, 7),
            shopId = shopId.value,
            categoryId = 12,
            price = null,
            seasonModel = null,
            colorId = null
        )
    )

    shopProducts.forEach {
        shopProductUseCase(
            shopId = shopId.value,
            sizeIds = it.sizeIds,
            categoryId = it.categoryId,
            productModel = it
        )
    }
}

private suspend fun Application.logAllCategoriesWithSubs() {
    val shopProductsCategoriesRepository by inject<ShopProductCategoryRepository>()

    shopProductsCategoriesRepository.getSuperCategories()
        .forEach {
            println()
            println(it.name)
            val children = it.children
            children?.forEach {
                val childs = it.children
                println(it.name)

                childs?.forEach {
                    print(" " + it.name + ",")
                }

                println()
            }
        }
}

private suspend fun Application.logAllSizes() {
    val sizes by inject<ShopProductSizesRepository>()

    println("Размеры")

    sizes.getAllSizes()
        .forEach {
            println()
            println(it.name)

            val children = it.children

            children.forEach {
                println(it.name)
            }
        }
}
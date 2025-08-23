package com.ducks.database

import com.ducks.features.shops.seller.domain.ShopProductInteractor
import com.ducks.features.shops.common.model.ShopModel
import com.ducks.features.shops.common.model.ShopProductModel
import com.ducks.features.shops.common.repository.ShopProductCategoryRepository
import com.ducks.features.shops.common.repository.ShopProductSizesRepository
import com.ducks.features.shops.common.repository.ShopProductsRepository
import com.ducks.features.shops.common.repository.ShopsRepository
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

    }
}

private suspend fun Application.searchSomeProducts() {
    val searchProductsUseCase by inject<ShopProductsRepository>()

    val result = searchProductsUseCase.searchWithQueryOrFilters(null, null, null, null, null, null, null, null)

    println("Продукты")
    result.products.forEach {
        println()
        println(it.name)
    }

    println("Размеры")
    result.filters.sizes.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }

    println("Цвета")
    result.filters.colors.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }

    println("Категории")
    result.filters.categories.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }

    println("Сезоны")
    result.filters.seasons.forEach {
        println()
        println(it.id.toString() + " " + it.name)
    }
}

private suspend fun Application.deleteAllAndInsertNewShops() {
    val shopsRepository by inject<ShopsRepository>()
    val shopProductUseCase by inject<ShopProductInteractor>()

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
            shopId = shopId.value,
            price = null,
        ),
        ShopProductModel(
            name = "Майка",
            description = "Обычная",
            imageUrls = emptyList(),
            brandName = "Zara",
            price = null,
            shopId = shopId.value,
        ),
        ShopProductModel(
            name = "Кроссовки",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            shopId = shopId.value,
            price = null,
        ),
        ShopProductModel(
            name = "Кофта",
            description = "Обычная",
            imageUrls = emptyList(),
            brandName = "Zara",
            shopId = shopId.value,
            price = null,
        ),
        ShopProductModel(
            name = "Туника",
            description = "Обычная",
            imageUrls = emptyList(),
            brandName = "Zara",
            shopId = shopId.value,
            price = null,
        ),
        ShopProductModel(
            name = "Брюки",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            shopId = shopId.value,
            price = null,
        ),
        ShopProductModel(
            name = "Штаны",
            description = "Велюровые",
            imageUrls = emptyList(),
            brandName = "Zara",
            shopId = shopId.value,
            price = null,
        )
    )
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
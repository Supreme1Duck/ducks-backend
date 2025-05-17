package com.ducks.database

import com.ducks.database.repository.ShopProductCategoryRepository
import com.ducks.dto.ShopProductCategoryDTO
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

// TODO Убрать
private fun Application.testFunction() {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val shopProductsCategoriesRepository by inject<ShopProductCategoryRepository>()

    val topLevelCategories = listOf(
        ShopProductCategoryDTO(
            name = "Мужчинам",
            description = "Мужская одежда",
            isSuperCategory = true,
        ),
        ShopProductCategoryDTO(
            name = "Женщинам",
            description = "Женская одежда",
            isSuperCategory = true,
        ),
        ShopProductCategoryDTO(
            name = "Аксессуары",
            description = "Аксессуары",
            isSuperCategory = true,
        ),
    )

//    coroutineScope.launch {
//        topLevelCategories.forEach {
//            shopProductsCategoriesRepository.insertSuperCategory(
//                it
//            )
//        }
//    }

//    val shopsRepository by inject<ShopsRepository>()
//    shopsRepository.deleteAll()
//    val shopProductsRepository by inject<ShopProductsRepository>()
//    val insertShopProductUseCase by inject<InsertShopProductUseCase>()

//    coroutineScope.launch {
//        val list = listOf(
//            ShopModel(
//                name = "ИП Малаш",
//                description = "Магазин одежды",
//                address = "Советская 12а",
//                photoUrls = listOf("https://supreme1duck.github.io/preview_1.png")
//            ),
//            ShopModel(
//                name = "Блинн",
//                description = "Магазин одежды",
//                address = "Советская 13а",
//                photoUrls = listOf("https://supreme1duck.github.io/preview_1.png")
//            ),
//            ShopModel(
//                name = "Пижон",
//                description = "Магазин одежды",
//                address = "Советская 14а",
//                photoUrls = listOf("https://supreme1duck.github.io/preview_1.png")
//            )
//        )
//
//        list.forEach {
//            shopsRepository.insert(it)
//        }
//
//        delay(5000L)
//
//        val lastShop = shopsRepository.insert(
//            ShopModel(
//                name = "Новый элемент",
//                description = "Магазин одежды",
//                address = "Советская 14а",
//                photoUrls = listOf("https://supreme1duck.github.io/preview_1.png")
//            )
//        )


/*        insertShopProductUseCase(
            shopId = lastShop.id.value,
            categoryId = ,
            productModel = ShopProductModel(
                name = "Лодочки",
                description = "Описание Описание Описание Описание Описание Описание",
                price = BigDecimal(1222),
                imageUrls = listOf(""),
                brandName = "ZARA",
            )
        )

        shopProductsRepository.insertProduct(
            shopEntity = lastShop,
            shopProductModel = ShopProductModel(
                name = "Лодочки 2",
                description = "Описание Описание Описание Описание Описание Описание",
                price = BigDecimal(1333),
                imageUrls = listOf(""),
                brandName = "ZARA",
            )
        )

        shopProductsRepository.insertProduct(
            shopEntity = lastShop,
            shopProductModel = ShopProductModel(
                name = "Лодочки 3",
                description = "Описание Описание Описание Описание Описание Описание",
                price = BigDecimal(1444),
                imageUrls = listOf(""),
                brandName = "ZARA",
            )
        )*/

//        shopsRepository.getAll()
//            .onEach {
//                println("Products for shop #${it.id} -> ${it.products}")
//            }
//    }
}
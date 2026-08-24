package com.ducks.service

import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.seller.domain.CoffeeShopImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.hours

class CoffeeShopDeleteUnusedImagesService(
    private val shopImageRepository: CoffeeShopImageRepository,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun invoke() {
        scope.launch {
            while (true) {
                deleteUnusedShopImages()
                deleteUnusedProductImages()

                delay(24.hours)
            }
        }
    }

    private suspend fun deleteUnusedShopImages() {
        // Запросы в хранилище держим снаружи транзакции: незачем занимать соединение
        // с базой на время сетевых вызовов к S3.
        val savedImages = shopImageRepository.listShopImageNames()

        val usedImages = newSuspendedTransaction {
            CoffeeShopTable.select(
                CoffeeShopTable.imageUrls
            ).flatMap {
                it[CoffeeShopTable.imageUrls] ?: emptyList()
            }
        }.mapTo(mutableSetOf()) {
            // Сравниваем по имени файла, а не по URL целиком: в базе могут лежать ссылки
            // и на старую раздачу с сервера, и на хранилище.
            it.substringAfterLast("/")
        }

        savedImages
            .filter { it !in usedImages }
            .forEach { shopImageRepository.deleteImage(it) }
    }

    private suspend fun deleteUnusedProductImages() {
        val savedImages = shopImageRepository.listProductImageNames()

        val usedImages = newSuspendedTransaction {
            CoffeeProductTable.select(
                CoffeeProductTable.imageUrl
            ).map {
                it[CoffeeProductTable.imageUrl]
            }
        }.mapTo(mutableSetOf()) {
            it.substringAfterLast("/")
        }

        savedImages
            .filter { it !in usedImages }
            .forEach { shopImageRepository.deleteProductImage(it) }
    }
}

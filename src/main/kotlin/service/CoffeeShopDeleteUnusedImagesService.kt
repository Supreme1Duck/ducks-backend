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
import java.io.File
import kotlin.time.Duration.Companion.hours

class CoffeeShopDeleteUnusedImagesService(
    private val shopImageRepository: CoffeeShopImageRepository,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun invoke() {
        scope.launch {
            while (true) {
                newSuspendedTransaction {
                    val savedImages = findSavedShopsImages()

                    val allShopsImages = CoffeeShopTable.select(
                        CoffeeShopTable.imageUrls
                    ).flatMap {
                        it[CoffeeShopTable.imageUrls] ?: emptyList()
                    }.map {
                        it.substringAfterLast("/")
                    }

                    val listToDelete = savedImages.filter { it !in allShopsImages }

                    listToDelete.forEach {
                        shopImageRepository.deleteImage(it)
                    }
                }

                newSuspendedTransaction {
                    val savedImages = findSavedProductImages()

                    val allProductsImages = CoffeeProductTable.select(
                        CoffeeProductTable.imageUrl
                    ).map {
                        it[CoffeeProductTable.imageUrl]
                    }.map {
                        it.substringAfterLast("/")
                    }

                    val listToDelete = savedImages.filter { it !in allProductsImages }

                    listToDelete.forEach {
                        shopImageRepository.deleteProductImage(it)
                    }
                }

                delay(24.hours)
            }
        }
    }

    private fun findSavedShopsImages(): List<String> {
        val folder = File("coffee-shops/images")

        return if (folder.exists() && folder.isDirectory) {
            folder.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun findSavedProductImages(): List<String> {
        val folder = File("coffee-shops/products/images")

        return if (folder.exists() && folder.isDirectory) {
            folder.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?: emptyList()
        } else {
            emptyList()
        }
    }
}
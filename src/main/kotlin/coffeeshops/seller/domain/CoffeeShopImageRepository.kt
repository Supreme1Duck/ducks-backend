package com.ducks.coffeeshops.seller.domain

import com.ducks.coffeeshops.database.CoffeeShopTable
import com.ducks.common.data.DeleteImageResult
import com.ducks.common.data.SaveImageResult
import io.ktor.http.content.*
import org.jetbrains.exposed.v1.jdbc.select
import java.io.File
import java.util.*

class CoffeeShopImageRepository {

    private val allowedExtensions = listOf("jpg", "jpeg", "png")

    private val shopsFilePath = "coffee-shops/images"
    private val productsFilePath = "coffee-shops/products/images"

    fun saveImage(fileItem: PartData.FileItem): SaveImageResult {
        val originalName = fileItem.originalFileName ?: "unknown"
        val fileExtension = originalName.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        // TODO сделать полный улр
        val imageUrl = "http://localhost:8080/coffee-shops/images/${UUID.randomUUID()}.$fileExtension"
        val file = File(shopsFilePath, imageUrl)

        fileItem.streamProvider().use { its ->
            file.outputStream().use { os ->
                its.copyTo(os)
            }
        }

        return SaveImageResult.Success(imageUrl)
    }

    fun saveProductImage(fileItem: PartData.FileItem): SaveImageResult {
        val originalName = fileItem.originalFileName ?: "unknown"
        val fileExtension = originalName.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        // TODO сделать полный улр
        val imageUrl = "http://localhost:8080/coffee-shops/products/images/${UUID.randomUUID()}.$fileExtension"
        val file = File(productsFilePath, imageUrl)

        fileItem.streamProvider().use { its ->
            file.outputStream().use { os ->
                its.copyTo(os)
            }
        }

        return SaveImageResult.Success(imageUrl)
    }

    fun deleteImage(
        shopId: Long,
        imageUrl: String
    ): DeleteImageResult {
        val fileExtension = imageUrl.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return DeleteImageResult.UnsupportedImageType
        }

        if (!shopHasImage(shopId, imageUrl)) {
            return DeleteImageResult.TryToDeleteAlienFile
        }

        val file = File("$shopsFilePath/$imageUrl")

        if (!file.exists()) {
            return DeleteImageResult.FileNotFound
        }

        val deleted = file.delete()

        return if (deleted) {
            DeleteImageResult.Success
        } else {
            DeleteImageResult.InternalError
        }
    }

    fun deleteProductImage(
        imageUrl: String
    ): DeleteImageResult {
        val fileExtension = imageUrl.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return DeleteImageResult.UnsupportedImageType
        }

        val file = File("$productsFilePath/$imageUrl")

        if (!file.exists()) {
            return DeleteImageResult.FileNotFound
        }

        val deleted = file.delete()

        return if (deleted) {
            DeleteImageResult.Success
        } else {
            DeleteImageResult.InternalError
        }
    }

    private fun shopHasImage(shopId: Long, imageUrl: String): Boolean {
        return CoffeeShopTable
            .select(CoffeeShopTable.imageUrls)
            .where { CoffeeShopTable.id eq shopId }
            .flatMap {
                it[CoffeeShopTable.imageUrls].orEmpty()
            }.contains(imageUrl)
    }
}
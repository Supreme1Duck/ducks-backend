package com.ducks.shops.seller.data

import com.ducks.shops.seller.data.model.DeleteImageResult
import com.ducks.shops.seller.data.model.SaveImageResult
import io.ktor.http.content.*
import java.io.File
import java.util.*

class ShopImageRepository {
    private val allowedExtensions = listOf("jpg", "jpeg", "png")

    fun saveImage(fileItem: PartData.FileItem): SaveImageResult {
        val originalName = fileItem.originalFileName ?: "unknown"
        val fileExtension = originalName.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        // TODO сделать полный улр
        val imageUrl = "http://localhost:8080/shops/images/${UUID.randomUUID()}.$fileExtension"
        val file = File("shops/images", imageUrl)

        fileItem.streamProvider().use { its ->
            file.outputStream().use { os ->
                its.copyTo(os)
            }
        }

        return SaveImageResult.Success(imageUrl)
    }

    fun deleteImage(imageUrl: String): DeleteImageResult {
        val fileExtension = imageUrl.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return DeleteImageResult.UnsupportedImageType
        }

        val file = File("shops/images/$imageUrl")

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
}
package com.ducks.features.coffeeshops.seller.domain

import com.ducks.common.data.DeleteImageResult
import com.ducks.common.data.SaveImageResult
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.select
import java.io.File
import java.util.*

class CoffeeShopImageRepository(
    private val ktor: HttpClient,
) {

    private val allowedExtensions = listOf("jpg", "jpeg", "png")

    private val shopsFilePath = "coffee-shops/images"
    private val productsFilePath = "coffee-shops/products/images"

    suspend fun saveImage(fileItem: PartData.FileItem): SaveImageResult {
        val originalName = fileItem.originalFileName ?: "unknown"
        val fileExtension = originalName.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        // TODO сделать полный улр
        val imagePath = "${UUID.randomUUID()}.jpg"
        val imageUrl = "http://localhost:8080/coffee-shops/images/$imagePath"

        val file = File(shopsFilePath, imagePath)

        file.parentFile.mkdirs()

        val imageBytes = withContext(Dispatchers.IO) {
            fileItem.streamProvider.invoke().readAllBytes()
        }

        file.writeBytes(imageBytes)

        return SaveImageResult.Success(imageUrl)
    }

    suspend fun saveProductImage(fileItem: PartData.FileItem): SaveImageResult {
        val originalName = fileItem.originalFileName ?: "unknown"
        val fileExtension = originalName.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        val imageBytes = withContext(Dispatchers.IO) {
            fileItem.streamProvider.invoke().readAllBytes()
        }

        val imageWithoutBackground = removeBackgroundOnImage(imageBytes)

        val imagePath = "${UUID.randomUUID()}.jpg"
        val imageUrl = "http://localhost:8080/coffee-shops/products/images/$imagePath"
        val file = File(productsFilePath, imagePath)

        file.parentFile.mkdirs()

        file.writeBytes(imageWithoutBackground)

        return SaveImageResult.Success(imageUrl)
    }

    private suspend fun removeBackgroundOnImage(image: ByteArray): ByteArray {
        val response = ktor.post("https://api.remove.bg/v1.0/removebg") {
            headers {
                append("X-Api-Key", "Han8KHaDNNMUZLmTWRbqyXnj")
            }

            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        "image_file",
                        image,
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"file\"")
                        }
                    )
                    append("size", "auto")
                }
            ))
        }

        return response.bodyAsBytes()
    }

    fun deleteImage(
        imageUrl: String
    ): DeleteImageResult {
        val fileExtension = imageUrl.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return DeleteImageResult.UnsupportedImageType
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
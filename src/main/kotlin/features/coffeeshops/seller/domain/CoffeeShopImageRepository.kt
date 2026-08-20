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
    private val baseUrl: String,
    private val photoroomApiKey: String,
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
        val imageUrl = "$baseUrl/coffee-shops/images/$imagePath"

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

        // Именно png: Photoroom возвращает картинку с вырезанным фоном, и прозрачность
        // нужна, чтобы товар лёг на любой фон в приложении. В jpg альфа-канала нет.
        val imagePath = "${UUID.randomUUID()}.png"
        val imageUrl = "$baseUrl/coffee-shops/products/images/$imagePath"
        val file = File(productsFilePath, imagePath)

        file.parentFile.mkdirs()

        file.writeBytes(imageWithoutBackground)

        return SaveImageResult.Success(imageUrl)
    }

    private suspend fun removeBackgroundOnImage(image: ByteArray): ByteArray {
        val response = ktor.post("https://sdk.photoroom.com/v1/segment") {
            headers {
                append("x-api-key", photoroomApiKey)
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
                    // У Photoroom нет размера "auto" как у remove.bg: preview/medium/hd/full.
                    // hd — это 4 МП, для карточки товара с запасом; full (36 МП) раздувал
                    // и время ответа, и вес файла на диске.
                    append("size", "hd")
                    // Дефолт и так png, но формат тут принципиален: он даёт альфа-канал.
                    append("format", "png")
                    // Обрезать по границам объекта — карточки товаров получаются
                    // одинаково скомпонованными независимо от того, как сняли исходник.
                    append("crop", "true")
                    // Убрать цветной ореол от фона по краям объекта.
                    append("despill", "true")
                }
            ))
        }

        if (!response.status.isSuccess()) {
            // Иначе тело с ошибкой (JSON) молча сохранилось бы вместо картинки.
            error("Photoroom вернул ${response.status}: ${response.bodyAsText()}")
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
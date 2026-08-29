package com.ducks.features.coffeeshops.seller.domain

import com.ducks.common.data.DeleteImageResult
import com.ducks.common.data.SaveImageResult
import com.ducks.common.image.ProductImageNormalizer
import com.ducks.common.storage.S3Storage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.exception.SdkException
import java.util.*

class CoffeeShopImageRepository(
    private val ktor: HttpClient,
    private val s3: S3Storage,
    private val photoroomApiKey: String,
) {

    private val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")

    suspend fun saveImage(fileItem: PartData.FileItem): SaveImageResult {
        val fileExtension = extensionOf(fileItem.originalFileName)

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        val imageBytes = readImageBytes(fileItem)

        val key = "$SHOPS_PREFIX${UUID.randomUUID()}.$fileExtension"

        val imageUrl = s3.put(key, imageBytes, contentTypeOf(fileExtension))

        return SaveImageResult.Success(imageUrl)
    }

    suspend fun saveProductImage(fileItem: PartData.FileItem): SaveImageResult =
        saveProductImage(readImageBytes(fileItem), fileItem.originalFileName)

    suspend fun saveProductImage(imageBytes: ByteArray, originalFileName: String?): SaveImageResult {
        val fileExtension = extensionOf(originalFileName)

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        val imageWithoutBackground = removeBackgroundOnImage(imageBytes)

        // Photoroom обрезает картинку по границам объекта, из-за чего у каждого товара
        // свой размер и свой масштаб в карточке. Приводим к общему холсту.
        val normalized = withContext(Dispatchers.Default) {
            ProductImageNormalizer.normalize(imageWithoutBackground)
        }

        val imageUrl = s3.put(productImageKey(), normalized, "image/png")

        return SaveImageResult.Success(imageUrl)
    }

    /**
     * Кладёт картинку товара в хранилище байт в байт: ни Photoroom, ни нормализации.
     * Нужна, когда картинку уже подготовили руками и любая наша обработка её только
     * испортит — например, Photoroom срезал у товара часть силуэта.
     *
     * Имя такого файла помечено [RAW_MARKER], и CoffeeShopNormalizeProductImagesService
     * его не трогает: иначе ручную картинку на ближайшем старте перезалило бы под общий
     * холст, а ссылка в базе сменилась бы сама собой.
     */
    suspend fun saveProductImageAsIs(fileItem: PartData.FileItem): SaveImageResult =
        saveProductImageAsIs(readImageBytes(fileItem), fileItem.originalFileName)

    /** Тот же путь для картинки, уже вычитанной из запроса — см. [saveProductImage]. */
    suspend fun saveProductImageAsIs(imageBytes: ByteArray, originalFileName: String?): SaveImageResult {
        val fileExtension = extensionOf(originalFileName)

        if (fileExtension !in allowedExtensions) {
            return SaveImageResult.UnsupportedFileType
        }

        val key = "$PRODUCTS_PREFIX${UUID.randomUUID()}$RAW_MARKER.$fileExtension"

        val imageUrl = s3.put(key, imageBytes, contentTypeOf(fileExtension))

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
                    // У Photoroom нет размера "auto" как у remove.bg: preview (0.25 МП),
                    // medium (1.5 МП), hd (4 МП), full (36 МП). Берём hd: это исходник
                    // для нашего масштабирования, и с 4 МП объект почти никогда не
                    // приходится растягивать вверх под холст 1024×1024. На трафик и
                    // хранилище это не влияет — наружу уходит уже наш холст, а не ответ
                    // Photoroom, и стоит вызов столько же.
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

    /**
     * Перезаливает уже сохранённую картинку товара, приведя её к текущему масштабу.
     * Возвращает новый URL или null, если исходник недоступен.
     *
     * Пишем под новым ключом, а не поверх старого: объекты отдаются с
     * `Cache-Control: immutable`, и подмена содержимого по тому же адресу оставила бы
     * у части клиентов старую картинку навсегда. Осиротевший файл через сутки уберёт
     * CoffeeShopDeleteUnusedImagesService.
     */
    suspend fun renormalizeProductImage(imageUrl: String): String? {
        val response = ktor.get(imageUrl)

        if (!response.status.isSuccess()) return null

        val bytes = response.bodyAsBytes()
        val normalized = withContext(Dispatchers.Default) {
            ProductImageNormalizer.normalize(bytes)
        }

        return s3.put(productImageKey(), normalized, "image/png")
    }

    /**
     * Именно png: Photoroom возвращает картинку с вырезанным фоном, и прозрачность нужна,
     * чтобы товар лёг на любой фон в приложении — в jpg альфа-канала нет.
     *
     * Ревизия нормализации в имени: по ней видно прямо из ссылки в базе, приведена
     * картинка к текущему масштабу или досталась от прошлого правила. Иначе для ответа
     * на этот вопрос пришлось бы качать из хранилища каждую картинку каталога.
     */
    private fun productImageKey(): String =
        "$PRODUCTS_PREFIX${UUID.randomUUID()}$NORMALIZED_MARKER.png"

    suspend fun listShopImageNames(): List<String> = listNames(SHOPS_PREFIX)

    suspend fun listProductImageNames(): List<String> = listNames(PRODUCTS_PREFIX)

    suspend fun deleteImage(imageName: String): DeleteImageResult =
        delete(SHOPS_PREFIX, imageName)

    suspend fun deleteProductImage(imageName: String): DeleteImageResult =
        delete(PRODUCTS_PREFIX, imageName)

    private suspend fun listNames(prefix: String): List<String> =
        s3.listKeys(prefix).map { it.substringAfterLast("/") }

    private suspend fun delete(prefix: String, imageName: String): DeleteImageResult {
        val fileExtension = imageName.substringAfterLast(".", "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return DeleteImageResult.UnsupportedImageType
        }

        val key = "$prefix$imageName"

        if (!s3.exists(key)) {
            return DeleteImageResult.FileNotFound
        }

        return try {
            s3.delete(key)
            DeleteImageResult.Success
        } catch (_: SdkException) {
            // Ловим именно SdkException, а не Exception: широкий catch внутри корутины
            // проглотил бы и CancellationException, сломав остановку сервиса.
            DeleteImageResult.InternalError
        }
    }

    /**
     * Публично, потому что читать часть запроса приходится и снаружи: в multipart
     * с несколькими полями файл нельзя оставить «на потом» — как только разбор дошёл
     * до следующей части, поток предыдущей уже недоступен.
     */
    suspend fun readImageBytes(fileItem: PartData.FileItem): ByteArray =
        withContext(Dispatchers.IO) {
            fileItem.streamProvider.invoke().readAllBytes()
        }

    private fun extensionOf(originalFileName: String?): String =
        (originalFileName ?: "unknown").substringAfterLast(".", "").lowercase()

    // Тип проставляется один раз и навсегда: объекты уезжают с `Cache-Control: immutable`,
    // и починить заголовок потом можно только перезаливкой под новым ключом.
    private fun contentTypeOf(extension: String): String = when (extension) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    companion object {
        /** Хвост имени у картинок товаров, приведённых к текущему масштабу. */
        const val NORMALIZED_MARKER = "-n${ProductImageNormalizer.REVISION}"

        /**
         * Хвост имени у картинок, залитых как есть. Метка нужна не для красоты: по ней
         * автонормализация отличает наши картинки от подготовленных руками и обходит
         * вторые стороной. В UUID такого сочетания не бывает — там только hex.
         */
        const val RAW_MARKER = "-raw"

        private const val SHOPS_PREFIX = "coffee-shops/images/"
        private const val PRODUCTS_PREFIX = "coffee-shops/products/images/"
    }
}

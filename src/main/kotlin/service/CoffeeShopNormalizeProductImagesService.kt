package com.ducks.service

import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.seller.domain.CoffeeShopImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory

/**
 * Догоняет картинки товаров до текущей ревизии нормализации.
 *
 * Ревизия зашита в имя файла, поэтому «что чинить» — это условие в SQL, а не обход
 * хранилища: в обычной ситуации выборка пуста и сервис не делает ровно ничего. Работы
 * у него появляются в двух случаях — остался хвост картинок, залитых до нормализации,
 * или в ProductImageNormalizer подняли REVISION, поменяв правило масштабирования.
 *
 * Photoroom при этом не дёргается: работаем с уже вырезанным PNG, кредиты не тратятся.
 */
class CoffeeShopNormalizeProductImagesService(
    private val shopImageRepository: CoffeeShopImageRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun invoke() {
        scope.launch {
            val outdated = newSuspendedTransaction {
                CoffeeProductTable
                    .select(CoffeeProductTable.id, CoffeeProductTable.imageUrl)
                    .where {
                        CoffeeProductTable.imageUrl notLike
                                "%${CoffeeShopImageRepository.NORMALIZED_MARKER}.png"
                    }
                    .map { it[CoffeeProductTable.id].value to it[CoffeeProductTable.imageUrl] }
            }

            if (outdated.isEmpty()) return@launch

            logger.info("Картинок товаров не в текущем масштабе: ${outdated.size}, привожу")

            var updated = 0

            outdated.forEach { (productId, imageUrl) ->
                if (imageUrl.isBlank()) return@forEach

                // Одна битая или недоступная картинка не должна ронять весь прогон:
                // остальные починятся сейчас, а эта — на следующем старте.
                val newUrl = try {
                    shopImageRepository.renormalizeProductImage(imageUrl)
                } catch (error: Exception) {
                    logger.warn("Не удалось привести к масштабу $imageUrl", error)
                    null
                } ?: return@forEach

                newSuspendedTransaction {
                    CoffeeProductTable.update({ CoffeeProductTable.id eq productId }) {
                        it[CoffeeProductTable.imageUrl] = newUrl
                    }
                }

                updated++
            }

            logger.info("Приведено к общему масштабу картинок товаров: $updated из ${outdated.size}")
        }
    }
}

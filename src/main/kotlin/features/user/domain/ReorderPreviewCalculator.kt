package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import com.ducks.features.user.data.dto.ReorderCartProductDTO
import com.ducks.features.user.data.dto.ReorderChangeDTO
import com.ducks.features.user.data.dto.ReorderChangeStatus
import com.ducks.features.user.data.dto.ReorderConstructorDTO
import com.ducks.features.user.data.dto.ReorderPreviewDTO
import com.ducks.features.user.data.dto.ReorderSizeDTO
import java.math.BigDecimal

/**
 * Чистая (без БД) логика сравнения ранее заказанных позиций с актуальным состоянием.
 * Данные о заказе и текущем состоянии подготавливает [ReorderPreviewRepository].
 */
class ReorderPreviewCalculator {

    /** Ранее заказанная позиция (снапшот на момент заказа). */
    data class OrderedLine(
        val productId: Long,
        val name: String,
        val imageUrl: String?,
        val size: CoffeeProductSizeDTO,
        val constructors: List<OrderedProductConstructorDBModel>,
        val quantity: Int,
        /** Цена позиции, сохранённая при оформлении заказа. */
        val price: BigDecimal,
    )

    /** Актуальная добавка, привязанная к продукту. */
    data class CurrentConstructor(
        val name: String,
        val price: BigDecimal?,
        val inStock: Boolean,
    )

    /** Актуальное состояние продукта. `null` в [calculate] означает, что продукт удалён. */
    data class CurrentProduct(
        val name: String,
        val imageUrl: String?,
        val inStock: Boolean,
        val sizes: List<CoffeeProductSizeDTO>,
        /** Привязанные к продукту добавки среди заказанных, по id. */
        val constructors: Map<Long, CurrentConstructor>,
    )

    fun calculate(
        lines: List<OrderedLine>,
        currentProducts: Map<Long, CurrentProduct?>,
    ): ReorderPreviewDTO {
        val changes = mutableListOf<ReorderChangeDTO>()
        val cart = mutableListOf<ReorderCartProductDTO>()

        for (line in lines) {
            val product = currentProducts[line.productId]
            val available = product != null && product.inStock && product.sizes.isNotEmpty()

            if (!available) {
                changes += ReorderChangeDTO(
                    productId = line.productId,
                    productName = product?.name ?: line.name,
                    imageUrl = product?.imageUrl ?: line.imageUrl,
                    status = ReorderChangeStatus.PRODUCT_UNAVAILABLE.value,
                    oldPrice = null,
                    newPrice = null,
                    oldSize = null,
                    newSize = null,
                    constructor = null,
                    removedConstructors = null,
                )
                continue
            }

            product!!

            // --- Размер ---
            val orderedSize = line.size
            val sameSize = product.sizes.firstOrNull { it.id == orderedSize.id }
            val chosenSize: CoffeeProductSizeDTO
            if (sameSize == null) {
                chosenSize = pickReplacementSize(product.sizes, orderedSize.price)
                changes += ReorderChangeDTO(
                    productId = line.productId,
                    productName = product.name,
                    imageUrl = product.imageUrl,
                    status = ReorderChangeStatus.SIZE_UNAVAILABLE.value,
                    oldPrice = orderedSize.price,
                    newPrice = chosenSize.price,
                    oldSize = orderedSize.toReorderSize(),
                    newSize = chosenSize.toReorderSize(),
                    constructor = null,
                    removedConstructors = null,
                )
            } else {
                chosenSize = sameSize
                val priceCmp = sameSize.price.compareTo(orderedSize.price)
                if (priceCmp != 0) {
                    changes += ReorderChangeDTO(
                        productId = line.productId,
                        productName = product.name,
                        imageUrl = product.imageUrl,
                        status = if (priceCmp > 0) ReorderChangeStatus.SIZE_PRICE_INCREASED.value
                        else ReorderChangeStatus.SIZE_PRICE_DECREASED.value,
                        oldPrice = orderedSize.price,
                        newPrice = sameSize.price,
                        oldSize = orderedSize.toReorderSize(),
                        newSize = sameSize.toReorderSize(),
                        constructor = null,
                        removedConstructors = null,
                    )
                }
            }

            // --- Добавки ---
            val removed = mutableListOf<ReorderConstructorDTO>()
            val cartConstructors = mutableListOf<ReorderConstructorDTO>()

            for (ordered in line.constructors) {
                val current = product.constructors[ordered.id]
                if (current == null || !current.inStock) {
                    removed += ReorderConstructorDTO(ordered.id, ordered.name, ordered.price)
                    continue
                }

                val currentDto = ReorderConstructorDTO(ordered.id, current.name, current.price)
                cartConstructors += currentDto

                val priceCmp = (current.price ?: BigDecimal.ZERO).compareTo(ordered.price ?: BigDecimal.ZERO)
                if (priceCmp != 0) {
                    changes += ReorderChangeDTO(
                        productId = line.productId,
                        productName = product.name,
                        imageUrl = product.imageUrl,
                        status = if (priceCmp > 0) ReorderChangeStatus.CONSTRUCTOR_PRICE_INCREASED.value
                        else ReorderChangeStatus.CONSTRUCTOR_PRICE_DECREASED.value,
                        oldPrice = ordered.price,
                        newPrice = current.price,
                        oldSize = null,
                        newSize = null,
                        constructor = currentDto,
                        removedConstructors = null,
                    )
                }
            }

            if (removed.isNotEmpty()) {
                changes += ReorderChangeDTO(
                    productId = line.productId,
                    productName = product.name,
                    imageUrl = product.imageUrl,
                    status = ReorderChangeStatus.CONSTRUCTOR_UNAVAILABLE.value,
                    oldPrice = null,
                    newPrice = null,
                    oldSize = null,
                    newSize = null,
                    constructor = null,
                    removedConstructors = removed,
                )
            }

            // --- Позиция в новой корзине ---
            val constructorsPrice = cartConstructors.sumOf { it.price ?: BigDecimal.ZERO }
            val linePrice = (chosenSize.price + constructorsPrice).times(line.quantity.toBigDecimal())

            cart += ReorderCartProductDTO(
                id = line.productId,
                name = product.name,
                imageUrl = product.imageUrl,
                size = chosenSize.toReorderSize(),
                constructors = cartConstructors.ifEmpty { null },
                quantity = line.quantity,
                price = linePrice,
            )
        }

        val oldTotal = lines.sumOf { it.price }
        val newTotal = cart.sumOf { it.price }
        val delta = newTotal - oldTotal

        return ReorderPreviewDTO(
            changes = changes,
            totalPriceDelta = if (delta.compareTo(BigDecimal.ZERO) == 0) null else delta,
            cart = cart,
        )
    }

    /**
     * Заказанного размера больше нет. Берём самый дорогой из размеров дешевле старого,
     * а если таких нет — самый дешёвый из оставшихся. [sizes] гарантированно непустой.
     */
    private fun pickReplacementSize(
        sizes: List<CoffeeProductSizeDTO>,
        oldPrice: BigDecimal,
    ): CoffeeProductSizeDTO {
        val cheaper = sizes.filter { it.price < oldPrice }
        return if (cheaper.isNotEmpty()) {
            cheaper.maxBy { it.price }
        } else {
            sizes.minBy { it.price }
        }
    }

    private fun CoffeeProductSizeDTO.toReorderSize() = ReorderSizeDTO(
        id = id,
        sizeName = sizeName,
        sizeValue = sizeValue,
        price = price,
    )
}

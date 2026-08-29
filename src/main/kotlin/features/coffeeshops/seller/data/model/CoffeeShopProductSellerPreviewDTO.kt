package features.coffeeshops.seller.data.model

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CoffeeShopProductSellerPreviewDTO(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val categoryId: Long,
    val inStock: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    val minutesToCook: Int?,
    val cooksInParallel: Boolean,
    val shopId: Long,
)

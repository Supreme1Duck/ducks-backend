package features.coffeeshops

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopSizeSerializer
import com.ducks.util.ducksJson
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoffeeProductSizeSerializationTest {

    /**
     * Размеры лежат json'ом в колонке продукта, и у товаров, заведённых до появления
     * скидок, ключа priceWithoutDiscount там нет. Без значения по умолчанию такой товар
     * перестал бы читаться целиком.
     */
    @Test
    fun `размер из старой записи читается без цены без скидки`() {
        val stored = """[{"id":"1","sizeName":null,"sizeValue":"300 мл","price":"5.00"}]"""

        val size = CoffeeShopSizeSerializer.deserialize(stored).single()

        // Сравнение через compareTo: BigDecimalSerializer читает "5.00" как 5.0,
        // а equals у BigDecimal учитывает ещё и scale.
        assertEquals(0, BigDecimal("5.00").compareTo(size.price))
        assertNull(size.priceWithoutDiscount)
    }

    /** Ключ приходит всегда — иначе у клиента два разных вида одного и того же размера. */
    @Test
    fun `в ответе цена без скидки есть и когда её нет`() {
        val json = ducksJson.encodeToString(
            CoffeeProductSizeDTO(id = "1", sizeName = null, sizeValue = "300 мл", price = BigDecimal("5.00"))
        )

        assertTrue(json.contains("\"priceWithoutDiscount\":null"), json)
    }

    @Test
    fun `цена без скидки переживает запись и чтение`() {
        val size = CoffeeProductSizeDTO(
            id = "1",
            sizeName = null,
            sizeValue = "300 мл",
            price = BigDecimal("5.00"),
            priceWithoutDiscount = BigDecimal("7.90"),
        )

        val restored = CoffeeShopSizeSerializer.deserialize(CoffeeShopSizeSerializer.serialize(listOf(size))).single()

        assertEquals(size.id, restored.id)
        assertEquals(size.sizeValue, restored.sizeValue)
        assertEquals(0, size.price.compareTo(restored.price))
        assertEquals(0, size.priceWithoutDiscount!!.compareTo(restored.priceWithoutDiscount!!))
    }
}

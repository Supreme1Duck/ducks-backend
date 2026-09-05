package features.coffeeshops

import com.ducks.features.coffeeshops.seller.routings.request.products.CoffeeProductSizeRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductSizeRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.lowestPrice
import com.ducks.features.coffeeshops.seller.routings.request.products.validatedSizes
import com.ducks.util.DucksBadRequestError
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CoffeeProductSizeValidationTest {

    private fun size(
        name: String? = "M",
        value: String = "300 мл",
        price: String = "5.00",
        priceWithoutDiscount: String? = null,
    ) = CoffeeProductSizeRequest(
        id = "1",
        sizeName = name,
        sizeValue = value,
        price = BigDecimal(price),
        priceWithoutDiscount = priceWithoutDiscount?.let { BigDecimal(it) },
    )

    @Test
    fun `цена от берётся по самому дешёвому размеру`() {
        val sizes = listOf(size(price = "7.90"), size(name = "S", price = "5.00"))

        assertEquals(BigDecimal("5.00"), sizes.validatedSizes().lowestPrice())
    }

    /**
     * Тот самый случай, из-за которого заводились бесплатные товары: BigDecimal("0.0")
     * не равен BigDecimal.ZERO, потому что у них разный scale.
     */
    @Test
    fun `нулевая цена не проходит в любом написании`() {
        listOf("0", "0.0", "0.00").forEach { zero ->
            assertFailsWith<DucksBadRequestError>("цена $zero должна отбиваться") {
                listOf(size(price = zero)).validatedSizes()
            }
        }
    }

    @Test
    fun `отрицательная цена не проходит`() {
        assertFailsWith<DucksBadRequestError> {
            listOf(size(price = "-1.00")).validatedSizes()
        }
    }

    @Test
    fun `единственный размер может приходить без названия`() {
        val sizes = listOf(size(name = null)).validatedSizes()

        assertNull(sizes.single().sizeName)
        assertEquals("300 мл", sizes.single().sizeValue)
    }

    /**
     * Выбирать нечего, поэтому подпись не сохраняем, даже если приложение её прислало:
     * иначе у товара с одним размером в карточке висит кнопка выбора с подписью.
     */
    @Test
    fun `у единственного размера название отбрасывается`() {
        assertNull(listOf(size(name = "M")).validatedSizes().single().sizeName)
    }

    @Test
    fun `при нескольких размерах название обязательно`() {
        listOf(null, " ").forEach { emptyName ->
            assertFailsWith<DucksBadRequestError>("название $emptyName должно отбиваться") {
                listOf(size(name = "S"), size(name = emptyName)).validatedSizes()
            }
        }
    }

    @Test
    fun `при нескольких размерах названия сохраняются`() {
        val sizes = listOf(size(name = "S"), size(name = "L")).validatedSizes()

        assertEquals(listOf("S", "L"), sizes.map { it.sizeName })
    }

    @Test
    fun `размер без объёма не проходит`() {
        assertFailsWith<DucksBadRequestError> {
            listOf(size(value = "")).validatedSizes()
        }
    }

    @Test
    fun `товар без размеров не проходит`() {
        assertFailsWith<DucksBadRequestError> {
            emptyList<CoffeeProductSizeRequest>().validatedSizes()
        }
    }

    @Test
    fun `цена без скидки сохраняется как есть`() {
        val size = listOf(size(price = "5.00", priceWithoutDiscount = "7.90")).validatedSizes().single()

        assertEquals(BigDecimal("7.90"), size.priceWithoutDiscount)
    }

    @Test
    fun `размер без скидки приходит без цены без скидки`() {
        assertNull(listOf(size()).validatedSizes().single().priceWithoutDiscount)
    }

    /**
     * Зачёркнутая цена ниже или равна текущей — не скидка: в карточке рядом встали бы
     * две одинаковые цены либо перечёркнутая оказалась бы дешевле той, что платят.
     */
    @Test
    fun `цена без скидки не ниже и не равна цене со скидкой`() {
        listOf("5.00", "5.0", "4.90").forEach { withoutDiscount ->
            assertFailsWith<DucksBadRequestError>("цена без скидки $withoutDiscount должна отбиваться") {
                listOf(size(price = "5.00", priceWithoutDiscount = withoutDiscount)).validatedSizes()
            }
        }
    }

    /** «От» считается по цене, которую платят, а не по зачёркнутой. */
    @Test
    fun `цена от не смотрит на цену без скидки`() {
        val sizes = listOf(
            size(name = "S", price = "5.00", priceWithoutDiscount = "9.90"),
            size(name = "M", price = "7.90"),
        )

        assertEquals(BigDecimal("5.00"), sizes.validatedSizes().lowestPrice())
    }

    @Test
    fun `правила обновления те же, что у создания`() {
        val sizes = listOf(
            UpdateCoffeeProductSizeRequest(
                id = "1",
                sizeName = "M",
                sizeValue = "300 мл",
                price = BigDecimal.ZERO,
            )
        )

        assertFailsWith<DucksBadRequestError> { sizes.validatedSizes() }
    }
}

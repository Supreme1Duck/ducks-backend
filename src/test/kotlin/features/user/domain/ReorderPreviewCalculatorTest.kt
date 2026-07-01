package features.user.domain

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import com.ducks.features.user.data.dto.ReorderChangeStatus
import com.ducks.features.user.domain.ReorderPreviewCalculator
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReorderPreviewCalculatorTest {

    private val calc = ReorderPreviewCalculator()

    // --- helpers ---

    private fun bd(value: String) = BigDecimal(value)

    private fun assertBD(expected: String, actual: BigDecimal?, msg: String = "") {
        assertNotNull(actual, "$msg (expected $expected, got null)")
        assertEquals(0, bd(expected).compareTo(actual), "$msg expected $expected but got $actual")
    }

    private fun size(id: String, price: String, name: String? = "M", value: String = "200 мл") =
        CoffeeProductSizeDTO(id = id, sizeName = name, sizeValue = value, price = bd(price))

    private fun orderedConstructor(id: Long, price: String?, name: String = "c$id") =
        OrderedProductConstructorDBModel(id = id, name = name, price = price?.let { bd(it) })

    private fun currentConstructor(price: String?, inStock: Boolean = true, name: String = "c") =
        ReorderPreviewCalculator.CurrentConstructor(name = name, price = price?.let { bd(it) }, inStock = inStock)

    private fun line(
        productId: Long = 1,
        size: CoffeeProductSizeDTO,
        constructors: List<OrderedProductConstructorDBModel> = emptyList(),
        quantity: Int = 1,
        price: String,
        name: String = "product-$productId",
        imageUrl: String? = "img-$productId",
    ) = ReorderPreviewCalculator.OrderedLine(
        productId = productId,
        name = name,
        imageUrl = imageUrl,
        size = size,
        constructors = constructors,
        quantity = quantity,
        price = bd(price),
    )

    private fun currentProduct(
        sizes: List<CoffeeProductSizeDTO>,
        inStock: Boolean = true,
        constructors: Map<Long, ReorderPreviewCalculator.CurrentConstructor> = emptyMap(),
        name: String = "product",
        imageUrl: String? = "img",
    ) = ReorderPreviewCalculator.CurrentProduct(
        name = name,
        imageUrl = imageUrl,
        inStock = inStock,
        sizes = sizes,
        constructors = constructors,
    )

    // --- нет изменений ---

    @Test
    fun `без изменений — пустой список изменений, корзина копирует заказ, дельта null`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        val current = mapOf(1L to currentProduct(sizes = listOf(size("1", "12.0"))))

        val result = calc.calculate(listOf(line), current)

        assertTrue(result.changes.isEmpty())
        assertNull(result.totalPriceDelta)
        assertEquals(1, result.cart.size)
        val item = result.cart.first()
        assertEquals(1L, item.id)
        assertEquals("1", item.size.id)
        assertBD("12.0", item.price)
        assertNull(item.constructors)
    }

    @Test
    fun `пустой заказ — всё пусто, дельта null`() {
        val result = calc.calculate(emptyList(), emptyMap())

        assertTrue(result.changes.isEmpty())
        assertTrue(result.cart.isEmpty())
        assertNull(result.totalPriceDelta)
    }

    // --- размер ---

    @Test
    fun `цена размера выросла — статус SIZE_PRICE_INCREASED`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        val current = mapOf(1L to currentProduct(sizes = listOf(size("1", "15.0"))))

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.SIZE_PRICE_INCREASED.value, change.status)
        assertBD("12.0", change.oldPrice)
        assertBD("15.0", change.newPrice)
        assertBD("12.0", change.oldSize!!.price)
        assertBD("15.0", change.newSize!!.price)
        assertBD("15.0", result.cart.single().price)
        assertBD("3.0", result.totalPriceDelta)
    }

    @Test
    fun `цена размера упала — статус SIZE_PRICE_DECREASED`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        val current = mapOf(1L to currentProduct(sizes = listOf(size("1", "10.0"))))

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.SIZE_PRICE_DECREASED.value, change.status)
        assertBD("12.0", change.oldPrice)
        assertBD("10.0", change.newPrice)
        assertBD("-2.0", result.totalPriceDelta)
    }

    @Test
    fun `размера нет — берём ближайший дешевле старого`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        // дешевле 12: 8 и 10 -> берём самый дорогой из них (10)
        val current = mapOf(
            1L to currentProduct(sizes = listOf(size("2", "8.0"), size("3", "10.0"), size("4", "20.0")))
        )

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.SIZE_UNAVAILABLE.value, change.status)
        assertEquals("1", change.oldSize!!.id)
        assertEquals("3", change.newSize!!.id)
        assertBD("10.0", change.newPrice)
        assertEquals("3", result.cart.single().size.id)
        assertBD("-2.0", result.totalPriceDelta)
    }

    @Test
    fun `размера нет и дешевле нет — берём самый дешёвый из оставшихся`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        val current = mapOf(
            1L to currentProduct(sizes = listOf(size("2", "15.0"), size("3", "20.0")))
        )

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.SIZE_UNAVAILABLE.value, change.status)
        assertEquals("2", change.newSize!!.id)
        assertBD("15.0", result.cart.single().price)
        assertBD("3.0", result.totalPriceDelta)
    }

    // --- добавки ---

    @Test
    fun `цена добавки выросла — статус CONSTRUCTOR_PRICE_INCREASED, конструктор с новой ценой`() {
        val line = line(
            size = size("1", "12.0"),
            constructors = listOf(orderedConstructor(5, "1.0")),
            price = "13.0",
        )
        val current = mapOf(
            1L to currentProduct(
                sizes = listOf(size("1", "12.0")),
                constructors = mapOf(5L to currentConstructor("2.0")),
            )
        )

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.CONSTRUCTOR_PRICE_INCREASED.value, change.status)
        assertBD("1.0", change.oldPrice)
        assertBD("2.0", change.newPrice)
        assertEquals(5L, change.constructor!!.id)
        assertBD("2.0", change.constructor!!.price)
        // цена позиции = размер 12 + добавка 2
        assertBD("14.0", result.cart.single().price)
        assertBD("1.0", result.totalPriceDelta)
    }

    @Test
    fun `цена добавки упала — статус CONSTRUCTOR_PRICE_DECREASED`() {
        val line = line(
            size = size("1", "12.0"),
            constructors = listOf(orderedConstructor(5, "1.0")),
            price = "13.0",
        )
        val current = mapOf(
            1L to currentProduct(
                sizes = listOf(size("1", "12.0")),
                constructors = mapOf(5L to currentConstructor("0.5")),
            )
        )

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.CONSTRUCTOR_PRICE_DECREASED.value, change.status)
        assertBD("12.5", result.cart.single().price)
    }

    @Test
    fun `добавка откреплена от продукта — статус CONSTRUCTOR_UNAVAILABLE, из корзины убрана`() {
        val line = line(
            size = size("1", "12.0"),
            constructors = listOf(orderedConstructor(5, "1.0")),
            price = "13.0",
        )
        // добавки нет в актуальной карте продукта
        val current = mapOf(1L to currentProduct(sizes = listOf(size("1", "12.0"))))

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.CONSTRUCTOR_UNAVAILABLE.value, change.status)
        assertEquals(listOf(5L), change.removedConstructors!!.map { it.id })
        assertNull(result.cart.single().constructors)
        assertBD("12.0", result.cart.single().price)
        assertBD("-1.0", result.totalPriceDelta)
    }

    @Test
    fun `добавка не в наличии — статус CONSTRUCTOR_UNAVAILABLE`() {
        val line = line(
            size = size("1", "12.0"),
            constructors = listOf(orderedConstructor(5, "1.0")),
            price = "13.0",
        )
        val current = mapOf(
            1L to currentProduct(
                sizes = listOf(size("1", "12.0")),
                constructors = mapOf(5L to currentConstructor("1.0", inStock = false)),
            )
        )

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.CONSTRUCTOR_UNAVAILABLE.value, change.status)
        assertEquals(listOf(5L), change.removedConstructors!!.map { it.id })
    }

    @Test
    fun `цена добавки изменилась с null на значение — считается ростом`() {
        val line = line(
            size = size("1", "12.0"),
            constructors = listOf(orderedConstructor(5, null)),
            price = "12.0",
        )
        val current = mapOf(
            1L to currentProduct(
                sizes = listOf(size("1", "12.0")),
                constructors = mapOf(5L to currentConstructor("2.0")),
            )
        )

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.CONSTRUCTOR_PRICE_INCREASED.value, change.status)
        assertNull(change.oldPrice)
        assertBD("2.0", change.newPrice)
    }

    // --- продукт недоступен ---

    @Test
    fun `продукт удалён — статус PRODUCT_UNAVAILABLE, из корзины убран, имя из снапшота`() {
        val line = line(size = size("1", "12.0"), price = "12.0", name = "Старое имя")
        val current = mapOf<Long, ReorderPreviewCalculator.CurrentProduct?>(1L to null)

        val result = calc.calculate(listOf(line), current)

        val change = result.changes.single()
        assertEquals(ReorderChangeStatus.PRODUCT_UNAVAILABLE.value, change.status)
        assertEquals("Старое имя", change.productName)
        assertTrue(result.cart.isEmpty())
        assertBD("-12.0", result.totalPriceDelta)
    }

    @Test
    fun `продукт не в наличии — статус PRODUCT_UNAVAILABLE`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        val current = mapOf(1L to currentProduct(sizes = listOf(size("1", "12.0")), inStock = false))

        val result = calc.calculate(listOf(line), current)

        assertEquals(ReorderChangeStatus.PRODUCT_UNAVAILABLE.value, result.changes.single().status)
        assertTrue(result.cart.isEmpty())
    }

    @Test
    fun `у продукта нет размеров — статус PRODUCT_UNAVAILABLE`() {
        val line = line(size = size("1", "12.0"), price = "12.0")
        val current = mapOf(1L to currentProduct(sizes = emptyList()))

        val result = calc.calculate(listOf(line), current)

        assertEquals(ReorderChangeStatus.PRODUCT_UNAVAILABLE.value, result.changes.single().status)
        assertTrue(result.cart.isEmpty())
    }

    // --- комбинации ---

    @Test
    fun `несколько изменений для одной позиции — размер вырос, добавка подешевела, добавка удалена`() {
        val line = line(
            size = size("1", "12.0"),
            constructors = listOf(orderedConstructor(5, "1.0"), orderedConstructor(99, "0.5")),
            price = "13.5",
        )
        val current = mapOf(
            1L to currentProduct(
                sizes = listOf(size("1", "15.0")),
                constructors = mapOf(5L to currentConstructor("0.0")), // id 99 отсутствует -> удалена
            )
        )

        val result = calc.calculate(listOf(line), current)

        val statuses = result.changes.map { it.status }.toSet()
        assertEquals(
            setOf(
                ReorderChangeStatus.SIZE_PRICE_INCREASED.value,
                ReorderChangeStatus.CONSTRUCTOR_PRICE_DECREASED.value,
                ReorderChangeStatus.CONSTRUCTOR_UNAVAILABLE.value,
            ),
            statuses,
        )
        assertEquals(3, result.changes.size)
        // корзина: размер 15 + добавка 5 (0.0), 99 убрана
        val item = result.cart.single()
        assertEquals(listOf(5L), item.constructors!!.map { it.id })
        assertBD("15.0", item.price)
    }

    @Test
    fun `количество умножает цену позиции и дельту`() {
        val line = line(size = size("1", "10.0"), quantity = 3, price = "30.0")
        val current = mapOf(1L to currentProduct(sizes = listOf(size("1", "12.0"))))

        val result = calc.calculate(listOf(line), current)

        assertEquals(ReorderChangeStatus.SIZE_PRICE_INCREASED.value, result.changes.single().status)
        assertBD("36.0", result.cart.single().price) // 12 * 3
        assertBD("6.0", result.totalPriceDelta) // 36 - 30
    }

    @Test
    fun `дельта null когда изменения взаимно компенсируются`() {
        val lineA = line(productId = 1, size = size("1", "10.0"), price = "10.0")
        val lineB = line(productId = 2, size = size("1", "10.0"), price = "10.0")
        val current = mapOf(
            1L to currentProduct(sizes = listOf(size("1", "13.0"))), // +3
            2L to currentProduct(sizes = listOf(size("1", "7.0"))),  // -3
        )

        val result = calc.calculate(listOf(lineA, lineB), current)

        assertEquals(2, result.changes.size)
        assertNull(result.totalPriceDelta) // 20 - 20 = 0
    }
}

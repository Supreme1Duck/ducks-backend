package features.coffeeshops.seller

import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import util.BigDecimalSerializer
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCoffeeProductRequestTest {

    // Тот же (строгий) конфиг Json, что и на сервере (ServerPlugins.kt).
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(BigDecimal::class, BigDecimalSerializer)
        }
    }

    // Контракт: клиент шлёт нутриенты числом (или null/опускает поле),
    // а признак наличия — как "inStock".
    private val updateBody = """
        {
            "productId": 8,
            "name": "Каппучино",
            "description": "sadasdasd",
            "sizes": [
                { "id": "1", "sizeName": "M", "sizeValue": "200 мл", "price": 12.0 },
                { "id": "2", "sizeName": "L", "sizeValue": "300 мл", "price": 12.0 }
            ],
            "categoryId": 1,
            "imageUrl": "https://supreme1duck.github.io/coffee/cappucino.png",
            "minutesToCook": 2,
            "inStock": true,
            "constructors": [],
            "carbohydrates": 1,
            "protein": 20,
            "fats": 5
        }
    """.trimIndent()

    @Test
    fun `decodes update body with numeric nutrition and inStock field`() {
        val request = json.decodeFromString<UpdateCoffeeProductRequest>(updateBody)

        assertEquals(8L, request.productId)
        assertTrue(request.isInStock)          // из поля "inStock"
        assertEquals(1, request.carbohydrates)
        assertEquals(20, request.protein)
        assertEquals(5, request.fats)
    }

    @Test
    fun `nutrition is optional and defaults to null when omitted`() {
        val withoutNutrition = """
            {
                "productId": 8,
                "name": "Каппучино",
                "sizes": [
                    { "id": "1", "sizeName": "M", "sizeValue": "200 мл", "price": 12.0 }
                ],
                "categoryId": 1,
                "imageUrl": "https://supreme1duck.github.io/coffee/cappucino.png",
                "minutesToCook": 2,
                "inStock": true,
                "constructors": []
            }
        """.trimIndent()
        val request = json.decodeFromString<UpdateCoffeeProductRequest>(withoutNutrition)

        assertNull(request.carbohydrates)
        assertNull(request.protein)
        assertNull(request.fats)
    }

    @Test
    fun `create body also maps inStock field`() {
        val createBody = """
            {
                "name": "Каппучино",
                "sizes": [
                    { "id": "1", "sizeName": "M", "sizeValue": "200 мл", "price": 12.0 }
                ],
                "categoryId": 1,
                "imageUrl": "https://supreme1duck.github.io/coffee/cappucino.png",
                "minutesToCook": 2,
                "inStock": false,
                "carbohydrates": 1
            }
        """.trimIndent()

        val request = json.decodeFromString<CreateCoffeeProductRequest>(createBody)

        assertEquals("Каппучино", request.name)
        assertEquals(false, request.isInStock)   // из поля "inStock"
        assertEquals(1, request.carbohydrates)
        assertNull(request.protein)
    }
}

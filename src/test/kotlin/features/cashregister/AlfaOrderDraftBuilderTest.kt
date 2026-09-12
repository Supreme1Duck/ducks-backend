package com.ducks.features.cashregister

import com.ducks.util.DucksBadRequestError
import kotlinx.serialization.json.*
import kotlin.test.*

class AlfaOrderDraftBuilderTest {
    private fun line(total: String = "13.40", quantity: Int = 2) = AlfaOrderLine(
        42, "Капучино", "Большой 350 мл", listOf("Овсяное молоко", "Сироп"), quantity, total.toBigDecimal(),
    )

    @Test
    fun `в кассу уходят точные числовые суммы и полный состав`() {
        val draft = buildAlfaOrderDraft(12, "Ducks", true, "Без крышки", "13.40".toBigDecimal(),
            listOf(line()))
        val encoded = Json.encodeToString(AlfaOrderDraft.serializer(), draft)
        val item = Json.parseToJsonElement(encoded).jsonObject.getValue("items").jsonArray.single().jsonObject
        val info = item.getValue("info").jsonObject
        assertEquals("6.70", info.getValue("amount").jsonPrimitive.content)
        assertFalse(info.getValue("amount").jsonPrimitive.isString)
        assertEquals("13.40", item.getValue("sum").jsonPrimitive.content)
        assertEquals(2, info.getValue("quantity").jsonPrimitive.int)
        assertEquals("Капучино / Большой 350 мл / Овсяное молоко / Сироп", info.getValue("productName").jsonPrimitive.content)
        assertEquals("Ducks #12. С собой. Без крышки", draft.createOrder.getValue("comment").jsonPrimitive.content)
        assertEquals("BYN", draft.createOrder.getValue("currency").jsonPrimitive.content)
        assertEquals(draft, Json.decodeFromString<AlfaOrderDraft>(encoded))
        assertEquals(0, item.getValue("discount").jsonPrimitive.int)
        assertEquals(0, info.getValue("type").jsonPrimitive.int)
        assertTrue(encoded.contains("\"amount\":6.70"))
        assertTrue(encoded.contains("\"sum\":13.40"))
    }

    @Test
    fun `сохранённый JSON читается моделью без потери точности денег`() {
        val json = """{"createOrder":{"table":"Ducks","currency":"BYN","comment":"Ducks #12"},"items":[{"discount":0,"sum":9007199254740993.01,"info":{"id":42,"type":0,"productName":"Кофе","quantity":1,"amount":9007199254740993.01}}]}"""
        val draft = Json.decodeFromString<AlfaOrderDraft>(json)
        val item = draft.items.single()
        assertEquals("9007199254740993.01".toBigDecimal(), item.sum)
        assertEquals(item.sum, item.info.amount)
        assertEquals(json, Json.encodeToString(AlfaOrderDraft.serializer(), draft))
    }

    @Test
    fun `нечисловые денежные значения отклоняются`() {
        for (json in listOf("\"6.70\"", "null", "true", "{}", "[]")) {
            assertFailsWith<kotlinx.serialization.SerializationException> {
                Json.decodeFromString(AlfaMoneySerializer, json)
            }
        }
    }

    @Test
    fun `несовпадение суммы заказа и позиций требует проверки`() {
        assertFailsWith<DucksBadRequestError> {
            buildAlfaOrderDraft(12, "Ducks", false, null, "15.40".toBigDecimal(),
                listOf(line()))
        }
    }

    @Test
    fun `дробные копейки не округляются молча`() {
        assertFailsWith<DucksBadRequestError> {
            buildAlfaOrderDraft(12, "Ducks", false, null, "10.00".toBigDecimal(),
                listOf(line("10.00", 3)))
        }
    }

    @Test
    fun `пустые и отрицательные позиции не передаются`() {
        for (lines in listOf(emptyList(), listOf(line("0", 0)), listOf(line("-1", 1)))) {
            assertFailsWith<DucksBadRequestError> {
                buildAlfaOrderDraft(12, "Ducks", false, null, lines.sumOf { it.total }, lines)
            }
        }
    }
}

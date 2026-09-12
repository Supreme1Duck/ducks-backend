package com.ducks.features.cashregister

import com.ducks.util.ducksJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class AlfaTransferSerializationTest {
    @Test
    fun `результаты отправки используют закреплённые числовые коды`() {
        val codes = mapOf(
            0 to AlfaTransferResult.CREATED,
            1 to AlfaTransferResult.TRANSFERRED,
            2 to AlfaTransferResult.NEEDS_REVIEW,
            3 to AlfaTransferResult.CANCELLED,
        )
        for ((code, result) in codes) {
            val json = """{"operationToken":"token","result":$code,"cashboxOrderNumber":123}"""
            val request = ducksJson.decodeFromString<AlfaReportRequest>(json)
            assertEquals(result, request.result)
            assertEquals(json, ducksJson.encodeToString(request))
        }
    }

    @Test
    fun `состояния отправки используют те же коды что и база`() {
        val codes = mapOf(
            0 to AlfaTransferState.PENDING,
            1 to AlfaTransferState.IN_PROGRESS,
            2 to AlfaTransferState.TRANSFERRED,
            3 to AlfaTransferState.NEEDS_REVIEW,
            4 to AlfaTransferState.CANCELLED,
        )
        for ((code, state) in codes) {
            val dto = AlfaTransferDTO(11, state, null, true, 123)
            val json = ducksJson.encodeToString(dto)
            val encodedState = ducksJson.parseToJsonElement(json).jsonObject.getValue("state").jsonPrimitive
            assertEquals(code.toString(), encodedState.content)
            assertFalse(encodedState.isString)
            assertEquals(dto, ducksJson.decodeFromString<AlfaTransferDTO>(json))
        }
    }

    @Test
    fun `неизвестные коды и имена enum отклоняются`() {
        for (value in listOf("-1", "99", "\"CREATED\"")) {
            assertFailsWith<SerializationException> {
                ducksJson.decodeFromString<AlfaReportRequest>("""{"operationToken":"token","result":$value}""")
            }
        }
        for (value in listOf("-1", "99", "\"PENDING\"")) {
            assertFailsWith<SerializationException> {
                ducksJson.decodeFromString<AlfaTransferState>(value)
            }
        }
    }
}

package com.ducks.features.cashregister

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.math.BigDecimal

/** Денежное JSON-число для кассы: без кавычек и без преобразования через Double. */
object AlfaMoneySerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("AlfaMoney", PrimitiveKind.DOUBLE)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Денежные значения Alfa поддерживают только JSON")
        jsonEncoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Денежные значения Alfa поддерживают только JSON")
        val number = jsonDecoder.decodeJsonElement() as? JsonPrimitive
            ?: throw SerializationException("Ожидалось денежное JSON-число")
        if (number.isString) throw SerializationException("Ожидалось денежное JSON-число без кавычек")
        return number.content.toBigDecimalOrNull()
            ?: throw SerializationException("Некорректное денежное JSON-число")
    }
}

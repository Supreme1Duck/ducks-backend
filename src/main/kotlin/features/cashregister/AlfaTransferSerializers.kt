package com.ducks.features.cashregister

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AlfaTransferStateSerializer : KSerializer<AlfaTransferState> {
    override val descriptor = PrimitiveSerialDescriptor("AlfaTransferState", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: AlfaTransferState) = encoder.encodeInt(value.value)

    override fun deserialize(decoder: Decoder): AlfaTransferState {
        val code = decoder.decodeInt()
        return AlfaTransferState.fromValue(code)
            ?: throw SerializationException("Неизвестный код состояния отправки заказа: $code")
    }
}

object AlfaTransferResultSerializer : KSerializer<AlfaTransferResult> {
    override val descriptor = PrimitiveSerialDescriptor("AlfaTransferResult", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: AlfaTransferResult) = encoder.encodeInt(value.value)

    override fun deserialize(decoder: Decoder): AlfaTransferResult {
        val code = decoder.decodeInt()
        return AlfaTransferResult.fromValue(code)
            ?: throw SerializationException("Неизвестный код результата отправки заказа: $code")
    }
}

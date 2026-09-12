package com.ducks.features.cashregister

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal

/** В базе и API используется [value]. Коды не переиспользовать и не менять. */
@Serializable(with = AlfaTransferStateSerializer::class)
enum class AlfaTransferState(val value: Int) {
    PENDING(0),
    IN_PROGRESS(1),
    TRANSFERRED(2),
    NEEDS_REVIEW(3),
    CANCELLED(4),
    ;

    companion object {

        /** null — значение не из этого списка. */
        fun fromValue(value: Int): AlfaTransferState? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
data class AlfaCashRegisterSettings(val enabled: Boolean = false, val cafeTable: String = "Ducks")

@Serializable
data class AlfaClaimRequest(val operationToken: String)

/** В API используется [value], независимо от имени и порядка констант. */
@Serializable(with = AlfaTransferResultSerializer::class)
enum class AlfaTransferResult(val value: Int) {
    CREATED(0),
    TRANSFERRED(1),
    NEEDS_REVIEW(2),
    CANCELLED(3),
    ;

    companion object {
        fun fromValue(value: Int): AlfaTransferResult? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
data class AlfaReportRequest(
    val operationToken: String,
    val result: AlfaTransferResult,
    val cashboxOrderNumber: Long? = null,
)

@Serializable
data class AlfaTransferDTO(
    val orderId: Long,
    val state: AlfaTransferState,
    val cashboxOrderNumber: Long?,
    val sourceFinished: Boolean,
    val updatedAt: Long,
    val sourceCancelled: Boolean = false,
)

@Serializable
data class AlfaOrderDraft(
    val createOrder: JsonObject,
    // Тело /addItemsToOrder: {"orderNumber": <номер из кассы>, "items": items}.
    val items: List<AlfaOrderItem>,
)

@Serializable
data class AlfaOrderItem(
    val discount: Int,
    @Serializable(with = AlfaMoneySerializer::class)
    val sum: BigDecimal,
    val info: AlfaOrderItemInfo,
)

@Serializable
data class AlfaOrderItemInfo(
    val id: Long,
    val type: Int,
    val productName: String,
    val quantity: Int,
    @Serializable(with = AlfaMoneySerializer::class)
    val amount: BigDecimal,
)

@Serializable
data class AlfaClaimDTO(val transfer: AlfaTransferDTO, val draft: AlfaOrderDraft)

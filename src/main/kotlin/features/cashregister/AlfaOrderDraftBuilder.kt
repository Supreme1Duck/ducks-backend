package com.ducks.features.cashregister

import com.ducks.util.DucksBadRequestError
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.math.RoundingMode

internal data class AlfaOrderLine(
    val productId: Long,
    val name: String,
    val size: String,
    val additions: List<String>,
    val quantity: Int,
    val total: BigDecimal,
)

/** Использует сохранённые позиции заказа, а не изменяемое меню кофейни. */
internal fun buildAlfaOrderDraft(
    orderId: Long,
    cafeTable: String,
    isTakeaway: Boolean,
    comment: String?,
    orderPrice: BigDecimal,
    lines: List<AlfaOrderLine>,
): AlfaOrderDraft {
    if (lines.isEmpty() || lines.sumOf { it.total }.compareTo(orderPrice) != 0) {
        throw DucksBadRequestError("Состав и сумма заказа не совпадают. Требуется проверка перед передачей в кассу.")
    }
    val items = lines.map { line ->
        if (line.quantity <= 0 || line.total.signum() < 0 || line.productId !in 1..9_999_999_999_999L) {
            throw DucksBadRequestError("Некорректная позиция заказа для передачи в кассу.")
        }
        val amount = try {
            line.total.divide(line.quantity.toBigDecimal(), 2, RoundingMode.UNNECESSARY)
        } catch (_: ArithmeticException) {
            throw DucksBadRequestError("Цена позиции не выражается в копейках. Требуется проверка заказа.")
        }
        AlfaOrderItem(
            discount = 0,
            sum = line.total,
            info = AlfaOrderItemInfo(
                id = line.productId,
                type = 0,
                productName = (listOf(line.name, line.size) + line.additions)
                    .filter { it.isNotBlank() }.joinToString(" / "),
                quantity = line.quantity,
                amount = amount,
            ),
        )
    }
    return AlfaOrderDraft(
        createOrder = buildJsonObject {
            put("table", cafeTable)
            put("currency", "BYN")
            put("comment", listOfNotNull(
                "Ducks #$orderId",
                if (isTakeaway) "С собой" else "На месте",
                comment?.takeIf { it.isNotBlank() },
            ).joinToString(". "))
        },
        items = items,
    )
}

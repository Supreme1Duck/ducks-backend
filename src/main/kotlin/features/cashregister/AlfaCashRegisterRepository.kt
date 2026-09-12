package com.ducks.features.cashregister

import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.util.DucksBadRequestError
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class AlfaCashRegisterRepository {
    suspend fun getSettings(shopId: Long): AlfaCashRegisterSettings = supervisorScope {
        newSuspendedTransaction {
            readSettings(shopId)
        }
    }

    suspend fun setSettings(shopId: Long, request: AlfaCashRegisterSettings) {
        val table = request.cafeTable.trim()
        if (table.isEmpty() || table.length > 100) {
            throw DucksBadRequestError("Название стола должно содержать от 1 до 100 символов.")
        }
        supervisorScope {
            newSuspendedTransaction {
                lockShop(shopId)
                val exists = AlfaCashRegisterSettingsTable.select(AlfaCashRegisterSettingsTable.shopId)
                    .where { AlfaCashRegisterSettingsTable.shopId eq shopId }
                    .any()
                if (exists) {
                    AlfaCashRegisterSettingsTable.update({ AlfaCashRegisterSettingsTable.shopId eq shopId }) {
                        it[AlfaCashRegisterSettingsTable.enabled] = request.enabled
                        it[AlfaCashRegisterSettingsTable.cafeTable] = table
                    }
                } else {
                    AlfaCashRegisterSettingsTable.insert {
                        it[AlfaCashRegisterSettingsTable.shopId] = shopId
                        it[AlfaCashRegisterSettingsTable.enabled] = request.enabled
                        it[AlfaCashRegisterSettingsTable.cafeTable] = table
                    }
                }
            }
        }
    }

    /** Вызывается в той же транзакции, что и выдача заказа. Сеть здесь не используется. */
    fun enqueueCompletedOrder(orderId: Long, shopId: Long) {
        val alfaConfig = readSettings(shopId)
        if (!alfaConfig.enabled) return
        requireCompletedOrder(readOrder(shopId, orderId))
        val now = System.currentTimeMillis()
        AlfaOrderTransfersTable.insertIgnore {
            it[AlfaOrderTransfersTable.orderId] = orderId
            it[AlfaOrderTransfersTable.shopId] = shopId
            it[AlfaOrderTransfersTable.cafeTable] = alfaConfig.cafeTable
            it[AlfaOrderTransfersTable.state] = AlfaTransferState.PENDING
            it[AlfaOrderTransfersTable.createdAt] = now
            it[AlfaOrderTransfersTable.updatedAt] = now
        }
    }

    suspend fun listTransfers(shopId: Long): List<AlfaTransferDTO> = supervisorScope {
        newSuspendedTransaction {
            AlfaOrderTransfersTable.innerJoin(CoffeeOrdersTable)
                .select(
                    AlfaOrderTransfersTable.columns + listOf(
                        CoffeeOrdersTable.acceptedTime,
                        CoffeeOrdersTable.readyTime,
                        CoffeeOrdersTable.finishedTime,
                        CoffeeOrdersTable.isCancelledByClient,
                        CoffeeOrdersTable.isCancelledBySeller,
                        CoffeeOrdersTable.isExpired,
                        CoffeeOrdersTable.isNotPickedUp,
                    )
                )
                .where {
                    (AlfaOrderTransfersTable.shopId eq shopId) and
                            (((AlfaOrderTransfersTable.state eq AlfaTransferState.PENDING) and
                                    CoffeeOrdersTable.finishedTime.isNotNull() and
                                    CoffeeOrdersTable.acceptedTime.isNotNull() and CoffeeOrdersTable.readyTime.isNotNull() and
                                    (CoffeeOrdersTable.isCancelledByClient eq false) and
                                    (CoffeeOrdersTable.isCancelledBySeller eq false) and
                                    (CoffeeOrdersTable.isExpired eq false) and (CoffeeOrdersTable.isNotPickedUp eq false)) or
                                    (AlfaOrderTransfersTable.state inList listOf(
                                        AlfaTransferState.IN_PROGRESS,
                                        AlfaTransferState.NEEDS_REVIEW,
                                    )))
                }
                .orderBy(
                    AlfaOrderTransfersTable.createdAt to SortOrder.ASC,
                    AlfaOrderTransfersTable.orderId to SortOrder.ASC
                )
                .limit(100)
                .map { it.toDTO() }
        }
    }

    suspend fun getTransfer(shopId: Long, orderId: Long?): AlfaTransferDTO {
        val validatedOrderId = requireOrderId(orderId)
        return supervisorScope {
            newSuspendedTransaction {
                AlfaOrderTransfersTable.innerJoin(CoffeeOrdersTable)
                    .select(
                        AlfaOrderTransfersTable.columns + listOf(
                            CoffeeOrdersTable.acceptedTime,
                            CoffeeOrdersTable.readyTime,
                            CoffeeOrdersTable.finishedTime,
                            CoffeeOrdersTable.isCancelledByClient,
                            CoffeeOrdersTable.isCancelledBySeller,
                            CoffeeOrdersTable.isExpired,
                            CoffeeOrdersTable.isNotPickedUp,
                        )
                    )
                    .where {
                        (AlfaOrderTransfersTable.shopId eq shopId) and
                                (AlfaOrderTransfersTable.orderId eq validatedOrderId)
                    }
                    .firstOrNull()?.toDTO() ?: missingTransfer()
            }
        }
    }

    suspend fun startOrderSendingToCashRegister(shopId: Long, orderId: Long?, request: AlfaClaimRequest): AlfaClaimDTO {
        val validatedOrderId = requireOrderId(orderId)
        return supervisorScope {
            newSuspendedTransaction {
                val token = parseToken(request.operationToken)
                // На кофейню одновременно отправляется в кассу только один заказ.
                // Параллельный запрос не начинает отправку другого заказа, пока предыдущий
                // отправляется (IN_PROGRESS) или требует проверки в кассе (NEEDS_REVIEW).
                lockShop(shopId)
                val source = lockOrder(shopId, validatedOrderId)
                val transfer = lockTransfer(shopId, validatedOrderId)
                val state = transfer[AlfaOrderTransfersTable.state]
                val owner = transfer[AlfaOrderTransfersTable.operationToken]

                if (owner != null && owner != token) {
                    throw DucksBadRequestError("Передачу уже начал другой исполнитель. Требуется сверка с кассой.")
                }
                if (state != AlfaTransferState.PENDING) {
                    // Повторный запрос на начало отправки возвращает сохранённые данные отправки заказа в кассу.
                    // Повторять HTTP-запрос к кассе на основании этого ответа нельзя.
                    val payload = transfer[AlfaOrderTransfersTable.payload]
                        ?: throw DucksBadRequestError("Передача недоступна.")
                    return@newSuspendedTransaction AlfaClaimDTO(
                        transfer.toDTO(source), Json.decodeFromString(payload),
                    )
                }
                if (!readSettings(shopId).enabled) throw DucksBadRequestError("Альфа-касса отключена для кофейни.")
                requireCompletedOrder(source)
                val busy = AlfaOrderTransfersTable.select(AlfaOrderTransfersTable.orderId).where {
                    (AlfaOrderTransfersTable.shopId eq shopId) and
                            (AlfaOrderTransfersTable.state inList listOf(
                                AlfaTransferState.IN_PROGRESS,
                                AlfaTransferState.NEEDS_REVIEW,
                            ))
                }.any()
                if (busy) throw DucksBadRequestError("Сначала завершите или проверьте предыдущую передачу в кассу.")

                val draft = draft(validatedOrderId, transfer[AlfaOrderTransfersTable.cafeTable], source)
                val now = System.currentTimeMillis()
                AlfaOrderTransfersTable.update({ AlfaOrderTransfersTable.orderId eq validatedOrderId }) {
                    it[AlfaOrderTransfersTable.state] = AlfaTransferState.IN_PROGRESS
                    it[AlfaOrderTransfersTable.operationToken] = token
                    it[AlfaOrderTransfersTable.payload] = Json.encodeToString(draft)
                    it[AlfaOrderTransfersTable.updatedAt] = now
                }
                AlfaClaimDTO(AlfaTransferDTO(validatedOrderId, AlfaTransferState.IN_PROGRESS, null, true, now), draft)
            }
        }
    }

    suspend fun reportOrderSendingResult(shopId: Long, orderId: Long?, request: AlfaReportRequest): AlfaTransferDTO {
        val validatedOrderId = requireOrderId(orderId)
        return supervisorScope {
            newSuspendedTransaction {
                val token = parseToken(request.operationToken)
                val source = lockOrder(shopId, validatedOrderId)
                val transfer = lockTransfer(shopId, validatedOrderId)
                if (transfer[AlfaOrderTransfersTable.operationToken] != token) {
                    throw DucksBadRequestError("Неверный идентификатор передачи.")
                }
                val previousNumber = transfer[AlfaOrderTransfersTable.cashboxOrderNumber]
                val number = request.cashboxOrderNumber ?: previousNumber
                if ((number != null && number <= 0) ||
                    (previousNumber != null && number != previousNumber)
                ) {
                    throw DucksBadRequestError("Номер заказа в кассе не совпадает с сохранённым.")
                }
                if (request.result in listOf(
                        AlfaTransferResult.CREATED,
                        AlfaTransferResult.TRANSFERRED
                    ) && number == null
                ) {
                    throw DucksBadRequestError("Не передан номер заказа в кассе.")
                }
                val nextState = when (request.result) {
                    AlfaTransferResult.CREATED -> AlfaTransferState.IN_PROGRESS
                    AlfaTransferResult.TRANSFERRED -> AlfaTransferState.TRANSFERRED
                    AlfaTransferResult.NEEDS_REVIEW -> AlfaTransferState.NEEDS_REVIEW
                    AlfaTransferResult.CANCELLED -> AlfaTransferState.CANCELLED
                }
                if (transfer[AlfaOrderTransfersTable.state] in listOf(
                        AlfaTransferState.TRANSFERRED,
                        AlfaTransferState.CANCELLED,
                    )
                ) {
                    if (nextState != transfer[AlfaOrderTransfersTable.state]) {
                        throw DucksBadRequestError("Передача уже завершена.")
                    }
                    return@newSuspendedTransaction transfer.toDTO(source)
                }
                val now = System.currentTimeMillis()
                AlfaOrderTransfersTable.update({ AlfaOrderTransfersTable.orderId eq validatedOrderId }) {
                    it[AlfaOrderTransfersTable.state] = nextState
                    it[AlfaOrderTransfersTable.cashboxOrderNumber] = number
                    it[AlfaOrderTransfersTable.updatedAt] = now
                }
                AlfaTransferDTO(
                    validatedOrderId,
                    nextState,
                    number,
                    source[CoffeeOrdersTable.finishedTime] != null,
                    now,
                    source.isCancelled(),
                )
            }
        }
    }

    private fun readSettings(shopId: Long): AlfaCashRegisterSettings = AlfaCashRegisterSettingsTable.selectAll()
        .where { AlfaCashRegisterSettingsTable.shopId eq shopId }.firstOrNull()?.let {
            AlfaCashRegisterSettings(
                it[AlfaCashRegisterSettingsTable.enabled],
                it[AlfaCashRegisterSettingsTable.cafeTable],
            )
        } ?: AlfaCashRegisterSettings()

    private fun lockShop(shopId: Long) {
        if (!lockAlfaShop(shopId)) throw DucksBadRequestError("Кофейня не найдена.")
    }

    private fun lockOrder(shopId: Long, orderId: Long): ResultRow {
        if (!lockOrderForAlfaTransfer(shopId, orderId)) missingTransfer()
        return readOrder(shopId, orderId)
    }

    private fun readOrder(shopId: Long, orderId: Long): ResultRow {
        return CoffeeOrdersTable.select(
            CoffeeOrdersTable.acceptedTime,
            CoffeeOrdersTable.readyTime,
            CoffeeOrdersTable.finishedTime,
            CoffeeOrdersTable.isCancelledByClient,
            CoffeeOrdersTable.isCancelledBySeller,
            CoffeeOrdersTable.isExpired,
            CoffeeOrdersTable.isNotPickedUp,
            CoffeeOrdersTable.price,
            CoffeeOrdersTable.isTakeaway,
            CoffeeOrdersTable.comment,
        ).where {
            (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
        }.first()
    }

    private fun lockTransfer(shopId: Long, orderId: Long): ResultRow {
        if (!lockAlfaTransfer(shopId, orderId)) missingTransfer()
        return AlfaOrderTransfersTable.selectAll()
            .where {
                (AlfaOrderTransfersTable.shopId eq shopId) and (AlfaOrderTransfersTable.orderId eq orderId)
            }
            .first()
    }

    private fun draft(orderId: Long, table: String, source: ResultRow): AlfaOrderDraft {
        val lines = CoffeeOrderedProductsTable.select(
            CoffeeOrderedProductsTable.productId,
            CoffeeOrderedProductsTable.productName,
            CoffeeOrderedProductsTable.selectedSize,
            CoffeeOrderedProductsTable.constructors,
            CoffeeOrderedProductsTable.quantity,
            CoffeeOrderedProductsTable.price,
        ).where {
            CoffeeOrderedProductsTable.orderId eq orderId
        }.orderBy(CoffeeOrderedProductsTable.id to SortOrder.ASC).map {
            val size = it[CoffeeOrderedProductsTable.selectedSize]
            AlfaOrderLine(
                productId = it[CoffeeOrderedProductsTable.productId],
                name = it[CoffeeOrderedProductsTable.productName],
                size = listOfNotNull(size.sizeName, size.sizeValue).filter { it.isNotBlank() }.joinToString(" "),
                additions = it[CoffeeOrderedProductsTable.constructors].orEmpty().map { addition -> addition.name },
                quantity = it[CoffeeOrderedProductsTable.quantity],
                total = it[CoffeeOrderedProductsTable.price]
                    ?: throw DucksBadRequestError("В позиции заказа отсутствует цена."),
            )
        }
        return buildAlfaOrderDraft(
            orderId,
            table,
            source[CoffeeOrdersTable.isTakeaway],
            source[CoffeeOrdersTable.comment],
            source[CoffeeOrdersTable.price],
            lines,
        )
    }

    private fun ResultRow.isCancelled(): Boolean = this[CoffeeOrdersTable.isCancelledByClient] ||
            this[CoffeeOrdersTable.isCancelledBySeller] ||
            this[CoffeeOrdersTable.isExpired] ||
            this[CoffeeOrdersTable.isNotPickedUp]

    private fun requireCompletedOrder(source: ResultRow) {
        if (source.isCancelled()) throw DucksBadRequestError("Отменённый или незабранный заказ нельзя передавать в кассу.")
        if (source[CoffeeOrdersTable.finishedTime] == null ||
            source[CoffeeOrdersTable.readyTime] == null ||
            source[CoffeeOrdersTable.acceptedTime] == null
        ) {
            throw DucksBadRequestError("Заказ ещё не выдан клиенту.")
        }
    }

    private fun ResultRow.toDTO(source: ResultRow = this): AlfaTransferDTO {
        val state = this[AlfaOrderTransfersTable.state]
        return AlfaTransferDTO(
            orderId = this[AlfaOrderTransfersTable.orderId].value,
            state = if (state == AlfaTransferState.PENDING && source.isCancelled()) AlfaTransferState.CANCELLED else state,
            cashboxOrderNumber = this[AlfaOrderTransfersTable.cashboxOrderNumber],
            sourceFinished = source[CoffeeOrdersTable.finishedTime] != null,
            sourceCancelled = source.isCancelled(),
            updatedAt = this[AlfaOrderTransfersTable.updatedAt],
        )
    }

    private fun parseToken(value: String): UUID = try {
        UUID.fromString(value)
            .also { if (!it.toString().equals(value, ignoreCase = true)) throw IllegalArgumentException() }
    } catch (_: IllegalArgumentException) {
        throw DucksBadRequestError("operationToken должен быть UUID.")
    }

    private fun requireOrderId(orderId: Long?): Long = orderId?.takeIf { it > 0 }
        ?: throw DucksBadRequestError("Некорректный id заказа.")

    private fun missingTransfer(): Nothing = throw DucksBadRequestError("Передача заказа не найдена.")
}

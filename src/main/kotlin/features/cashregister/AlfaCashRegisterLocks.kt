package com.ducks.features.cashregister

import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

// В используемой beta-версии Exposed forUpdate() в проверке на PostgreSQL не
// добавлял FOR UPDATE в SQL. Для очереди отправки заказов в кассу используем
// явные SQL-запросы с параметрами, блокирующие строки до завершения транзакции.
internal fun lockAlfaShop(shopId: Long): Boolean = lockAlfaRow(
    "SELECT id FROM ducks_coffee_shop_table WHERE id = ? FOR UPDATE", shopId,
)

internal fun lockOrderForAlfaTransfer(shopId: Long, orderId: Long): Boolean = lockAlfaRow(
    "SELECT id FROM ducks_coffee_orders_table WHERE coffee_shop_id = ? AND id = ? FOR UPDATE", shopId, orderId,
)

internal fun lockAlfaTransfer(shopId: Long, orderId: Long): Boolean = lockAlfaRow(
    "SELECT order_id FROM ducks_alfa_order_transfers WHERE shop_id = ? AND order_id = ? FOR UPDATE", shopId, orderId,
)

private fun lockAlfaRow(sql: String, vararg ids: Long): Boolean = TransactionManager.current()
    .exec(sql, ids.map { LongColumnType() to it }) { it.next() } ?: false

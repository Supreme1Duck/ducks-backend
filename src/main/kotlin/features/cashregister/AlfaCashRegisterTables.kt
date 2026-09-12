package com.ducks.features.cashregister

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import org.jetbrains.exposed.v1.core.Table

object AlfaCashRegisterSettingsTable : Table("ducks_alfa_cash_register_settings") {
    val shopId = reference("shop_id", CoffeeShopTable)
    val enabled = bool("enabled").default(false)
    val cafeTable = varchar("cafe_table", 100)
    override val primaryKey = PrimaryKey(shopId)
}

object AlfaOrderTransfersTable : Table("ducks_alfa_order_transfers") {
    val orderId = reference("order_id", CoffeeOrdersTable)
    val shopId = reference("shop_id", CoffeeShopTable)
    val cafeTable = varchar("cafe_table", 100)
    val state = integer("state").transform(
        wrap = { AlfaTransferState.fromValue(it) ?: error("Неизвестное состояние передачи: $it") },
        unwrap = { it.value },
    )
    // Не истекает по таймеру: потеря ответа кассы не означает, что заказ не создан.
    val operationToken = uuid("operation_token").nullable()
    val cashboxOrderNumber = long("cashbox_order_number").nullable()
    val payload = text("payload").nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(orderId)

    init {
        index(false, shopId, state, createdAt)
    }
}

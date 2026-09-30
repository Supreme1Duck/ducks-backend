package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.cashregister.lockOrderForAlfaTransfer
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update

/** Фиксирует выдачу в транзакции вызывающего кода. */
internal fun JdbcTransaction.completeCoffeeOrder(
    orderId: Long,
    shopId: Long,
) {
    if (!lockOrderForAlfaTransfer(shopId, orderId)) throw DucksBadRequestError("Заказ не найден.")
    val order = CoffeeOrdersTable
        .select(CoffeeOrdersTable.finishedTime, CoffeeOrdersTable.readyTime)
        .where { (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId) }
        .firstOrNull() ?: throw DucksBadRequestError("Попытка выдать несуществующий заказ!")

    if (order[CoffeeOrdersTable.finishedTime] != null) {
        throw DucksBadRequestError("Попытка выдать уже завершённый заказ!")
    }
    if (order[CoffeeOrdersTable.readyTime] == null) {
        throw DucksBadRequestError("Нельзя выдать неготовый заказ!")
    }

    CoffeeOrdersTable.update({ (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId) }) {
        it[finishedTime] = System.currentTimeMillis()
    }
}

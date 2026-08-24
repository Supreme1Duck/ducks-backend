package com.ducks.features.coffeeshops

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.jdbc.select

/**
 * Флаг временного закрытия ставит сам продавец из приложения, поэтому проверяем его
 * на сервере: без этого клиент со старым списком кофешопов мог получить слоты времени
 * и оформить заказ в закрытый кофешоп.
 *
 * Вызывать только внутри транзакции.
 */
fun checkShopIsNotTemporaryClosed(shopId: Long) {
    val shop = CoffeeShopTable
        .select(CoffeeShopTable.isTemporaryClosed, CoffeeShopTable.temporaryClosedReason)
        .where { CoffeeShopTable.id eq shopId }
        .map { it[CoffeeShopTable.isTemporaryClosed] to it[CoffeeShopTable.temporaryClosedReason] }
        .firstOrNull() ?: throw DucksBadRequestError("Кофешоп не найден.")

    val (isTemporaryClosed, reason) = shop

    if (!isTemporaryClosed) return

    throw DucksBadRequestError(
        reason?.takeIf { it.isNotBlank() }
            ?.let { "Кофешоп временно закрыт: $it" }
            ?: "Кофешоп временно закрыт и не принимает заказы."
    )
}

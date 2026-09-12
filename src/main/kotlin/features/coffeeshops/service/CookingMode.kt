package com.ducks.features.coffeeshops.service

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import org.jetbrains.exposed.v1.jdbc.select

/**
 * Сколько бариста на смене и как они разбирают заказ. Селлер переключает режим руками
 * по ходу дня, влияет он только на новые заказы: уже принятые хранят своё время
 * приготовления снимком и не пересчитываются.
 */
enum class CookingMode(val value: Int) {

    /** Один бариста: позиции складываются в очередь. */
    NORMAL(0),

    /** Двое делят один заказ между собой — заказ короче, но по-прежнему один за раз. */
    SPLIT_ORDER(1),
    ;

    companion object {

        /** null — значение не из этого списка. */
        fun fromValue(value: Int): CookingMode? = entries.firstOrNull { it.value == value }
    }
}

fun fetchCookingMode(shopId: Long): CookingMode = CoffeeShopTable
    .select(CoffeeShopTable.cookingMode)
    .where { CoffeeShopTable.id eq shopId }
    .firstOrNull()
    ?.let { CookingMode.fromValue(it[CoffeeShopTable.cookingMode]) }
    ?: CookingMode.NORMAL

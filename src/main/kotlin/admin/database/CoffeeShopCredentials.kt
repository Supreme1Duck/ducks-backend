package com.ducks.admin.database

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeShopCredentialsTable: LongIdTable("ducks_coffee_shop_credentials_table") {

    val shopId = reference("shop_id", CoffeeShopTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()

    val login = text("login").uniqueIndex()
    val password = text("password")
    val createdBy = reference("created_by", AdminsTable, onDelete = ReferenceOption.SET_NULL)

    val pinCode = text("pin_code")
    val pinFailedAttempts = integer("pin_failed_attempts").default(0)
    val pinLastFailedAt = long("pin_last_failed_at").nullable()
    val pinLockedUntil = long("pin_locked_until").nullable()
}
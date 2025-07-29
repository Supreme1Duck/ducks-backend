package com.ducks.admin.database

import com.ducks.coffeeshops.database.CoffeeShopTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeShopCredentialsTable: LongIdTable("ducks_coffee_shop_credentials_table") {

    val shopId = reference("shop_id", CoffeeShopTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()

    val login = text("login").uniqueIndex()
    val password = text("password")
    val createdBy = reference("created_by", AdminsTable, onDelete = ReferenceOption.SET_NULL)
}
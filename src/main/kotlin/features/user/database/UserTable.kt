package com.ducks.features.user.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object UserTable: LongIdTable("ducks_user_table") {

    val phoneNumber = text("phone_number").uniqueIndex()
    val name = text("name").nullable()
    val secondName = text("second_name").nullable()
}
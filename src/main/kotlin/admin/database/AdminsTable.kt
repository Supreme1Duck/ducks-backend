package com.ducks.admin.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object AdminsTable : LongIdTable("ducks_admin_table") {

    val name = text("name")
    val secondName = text("second_name")
}
package com.ducks.database.table

import org.jetbrains.exposed.v1.core.Table

object UserTable : Table("ducks_user_table") {
    private val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50)
    override val primaryKey = PrimaryKey(id)
}
package com.ducks.admin.repository

import com.ducks.admin.database.AdminsTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AdminRepository {

    fun isAdminExists(adminId: Long): Boolean {
        return transaction {
            AdminsTable.selectAll()
                .where { AdminsTable.id eq adminId }
                .empty()
                .not()
        }
    }

    suspend fun createAdmin(firstName: String, lastName: String): Long {
        return newSuspendedTransaction {
            AdminsTable.insertAndGetId {
                it[name] = firstName
                it[secondName] = lastName
            }.value
        }
    }
}
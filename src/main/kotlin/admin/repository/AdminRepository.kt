package com.ducks.admin.repository

import com.ducks.admin.database.AdminsTable
import com.ducks.admin.repository.result.LoginResult
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
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

    suspend fun createAdmin(firstName: String, lastName: String, login: String, password: String): Long {
        return newSuspendedTransaction {
            AdminsTable.insertAndGetId {
                it[name] = firstName
                it[secondName] = lastName
                it[AdminsTable.login] = login
                it[AdminsTable.password] = password
            }.value
        }
    }

    suspend fun login(login: String, password: String): LoginResult {
        val adminId = newSuspendedTransaction {
            AdminsTable
                .select(AdminsTable.id)
                .where {
                    (AdminsTable.login eq login) and (AdminsTable.password eq password)
                }
                .map { it[AdminsTable.id].value }
                .firstOrNull()
        }

        return adminId?.let { LoginResult.Success(it) } ?: LoginResult.InvalidCredentials
    }
}
package com.ducks.features.user.data

import com.ducks.features.user.database.UserTable
import com.ducks.features.user.model.UserDTO
import com.ducks.features.user.route.request.LoginRequest
import com.ducks.features.user.route.request.OtpRequest
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class UsersRepository {

    fun generateOtp(otpRequest: OtpRequest) {

    }

    suspend fun saveUserAndGetId(request: LoginRequest): Long {
        return newSuspendedTransaction {
            try {
                UserTable.insertAndGetId {
                    it[name] = request.firstName.toString()
                    it[secondName] = request.lastName.toString()
                    it[phoneNumber] = request.phoneNumber
                }.value
            } catch (e: Exception) {
                if (e.message?.contains("unique constraint") == true) {
                    getUserIdByPhone(request.phoneNumber)
                } else throw e
            }
        }
    }

    suspend fun getUserByCredentials(userId: Long, phoneNumber: String): UserDTO? {
        return newSuspendedTransaction {
            UserTable
                .selectAll()
                .where {
                    (UserTable.id eq userId) and (UserTable.phoneNumber eq phoneNumber)
                }
                .map {
                    it.mapToUserDTO()
                }.firstOrNull()
        }
    }

    private suspend fun getUserIdByPhone(phoneNumber: String): Long {
        return newSuspendedTransaction {
            UserTable
                .select(UserTable.id)
                .where {
                    UserTable.phoneNumber eq phoneNumber
                }
                .map { it[UserTable.id].value }
                .first()
        }
    }
}
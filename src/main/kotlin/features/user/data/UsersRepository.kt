package com.ducks.features.user.data

import com.ducks.features.user.database.UserTable
import com.ducks.features.user.model.UserDTO
import com.ducks.features.user.route.request.LoginRequest
import com.ducks.features.user.route.request.OtpRequest
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class UsersRepository {

    fun generateOtp(otpRequest: OtpRequest) {

    }

    suspend fun saveUser(request: LoginRequest) {
        newSuspendedTransaction {
            UserTable.insert {
                it[name] = request.firstName.toString()
                it[secondName] = request.lastName.toString()
                it[phoneNumber] = request.phoneNumber
            }
        }
    }

    suspend fun getUserByPhone(phoneNumber: String): UserDTO {
        return newSuspendedTransaction {
            UserTable
                .selectAll()
                .where {
                    UserTable.phoneNumber eq phoneNumber
                }
                .map {
                    it.mapToUserDTO()
                }.first()
        }
    }
}
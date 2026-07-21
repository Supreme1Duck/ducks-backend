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
import org.jetbrains.exposed.v1.jdbc.update

class UsersRepository {

    fun generateOtp(otpRequest: OtpRequest) {

    }

    suspend fun saveUserAndGetId(request: LoginRequest): Long {
        val firstName = request.firstName?.takeIf { it.isNotBlank() }
        val lastName = request.lastName?.takeIf { it.isNotBlank() }

        return newSuspendedTransaction {
            val existingUserId = UserTable
                .select(UserTable.id)
                .where {
                    UserTable.phoneNumber eq request.phoneNumber
                }
                .map { it[UserTable.id].value }
                .firstOrNull()
                ?: return@newSuspendedTransaction UserTable.insertAndGetId {
                    it[name] = firstName
                    it[secondName] = lastName
                    it[phoneNumber] = request.phoneNumber
                }.value

            // Имя приходит не в каждой авторизации, поэтому перезаписываем только то,
            // что реально передали, иначе стёрли бы уже сохранённое.
            if (firstName != null || lastName != null) {
                UserTable.update({ UserTable.id eq existingUserId }) {
                    firstName?.let { newName -> it[name] = newName }
                    lastName?.let { newSecondName -> it[secondName] = newSecondName }
                }
            }

            existingUserId
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

    suspend fun updateFcmToken(userId: Long, token: String) {
        newSuspendedTransaction {
            UserTable.update({ UserTable.id eq userId }) {
                it[fcmToken] = token
            }
        }
    }
}
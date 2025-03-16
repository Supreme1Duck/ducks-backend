package com.ducks.repository

import com.ducks.model.User
import java.util.*

class UserRepository {

    private val users = mutableSetOf<User>()

    fun getAllUsers(): Set<User> = users

    fun getUserById(id: UUID): User? {
        return users.firstOrNull { it.id == id }
    }

    fun getUserByPhoneNumber(phoneNumber: String): User? {
        return users.firstOrNull { it.phoneNumber == phoneNumber }
    }

    fun saveUser(user: User) {
        users.add(user)
    }
}
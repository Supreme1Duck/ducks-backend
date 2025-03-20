package com.ducks.mapper

import com.ducks.model.User
import com.ducks.routings.request.LoginRequest
import java.util.*

object UserMapper {

    fun loginRequestToUser(request: LoginRequest): User {
        return User(
            id = UUID.randomUUID(),
            phoneNumber = request.phoneNumber,
            firstName = request.firstName,
            lastName = request.lastName
        )
    }
}
package com.ducks.features.user.data

import com.ducks.features.user.database.UserTable
import com.ducks.features.user.model.UserDTO
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.mapToUserDTO(): UserDTO {
    return UserDTO(
        id = this[UserTable.id].value,
        phoneNumber = this[UserTable.phoneNumber],
        firstName = this[UserTable.name],
        lastName = this[UserTable.secondName],
    )
}
package com.ducks.admin.repository.result

sealed interface LoginResult {
    data object InvalidCredentials : LoginResult
    data class Success(val shopId: Long) : LoginResult
}
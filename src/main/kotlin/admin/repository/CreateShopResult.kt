package com.ducks.admin.repository

sealed interface CreateShopResult {

    data object Success : CreateShopResult
    data object AlreadyExists : CreateShopResult

}
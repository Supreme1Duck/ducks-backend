package com.ducks.admin.repository.result

sealed interface CreateShopResult {

    data object Success : CreateShopResult
    data object AlreadyExists : CreateShopResult

}
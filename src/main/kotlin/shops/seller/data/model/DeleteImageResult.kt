package com.ducks.shops.seller.data.model

sealed interface DeleteImageResult {
    data object UnsupportedImageType : DeleteImageResult
    data object FileNotFound : DeleteImageResult
    data object Success : DeleteImageResult
    data object InternalError : DeleteImageResult
}
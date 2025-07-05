package com.ducks.shops.seller.data.model

sealed interface SaveImageResult {

    data object UnsupportedFileType : SaveImageResult

    data class Success(val imageUrl: String) : SaveImageResult
}
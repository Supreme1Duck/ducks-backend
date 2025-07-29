package com.ducks.common.data

sealed interface SaveImageResult {

    data object UnsupportedFileType : SaveImageResult

    data class Success(val imageUrl: String) : SaveImageResult
}
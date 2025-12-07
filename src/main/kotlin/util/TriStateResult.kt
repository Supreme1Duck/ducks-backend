package com.ducks.util

sealed interface TriStateResult<out T> {
    data class Success<T>(
        val data: T,
    ) : TriStateResult<T>

    sealed interface Exception : TriStateResult<Nothing> {
        data class BadRequestException(
            val exception: String,
        ) : TriStateResult<Nothing>

        data class InternalServerException(
            val exception: String,
        ) : TriStateResult<Nothing>
    }
}

fun <T> TriStateResult<T>.isException(): Boolean {
    return this is TriStateResult.Exception
}

fun <T> TriStateResult<T>.exceptionOrNull(): TriStateResult.Exception? {
    return this as? TriStateResult.Exception
}

fun <T> TriStateResult<T>.successOrNull(): TriStateResult.Success<T>? {
    return this as? TriStateResult.Success
}

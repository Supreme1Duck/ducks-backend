package com.ducks.util

import org.jetbrains.exposed.v1.jdbc.SizedIterable

fun <T> SizedIterable<T>.optionalLimit(limit: Int?): SizedIterable<T> {
    return this.let { iterable ->
        limit?.let {
            iterable.limit(it)
        } ?: iterable
    }
}
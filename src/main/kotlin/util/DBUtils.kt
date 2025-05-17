package com.ducks.util

import org.jetbrains.exposed.sql.SizedIterable

fun <T> SizedIterable<T>.optionalLimit(limit: Int?): SizedIterable<T> {
    return this.let { iterable ->
        limit?.let {
            iterable.limit(it)
        } ?: iterable
    }
}
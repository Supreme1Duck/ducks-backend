package com.ducks.common

object ListHashGenerator {

    fun generateHash(list: List<Long>): Long? {
        if (list.isEmpty())
            return null

        var current = 1L

        list.forEach {
            val result = it * it * current
            current = result
        }

        return current
    }
}
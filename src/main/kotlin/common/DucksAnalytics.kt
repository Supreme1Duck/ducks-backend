@file:Suppress("LeakingThis")

package com.ducks.common

import java.io.File
import java.time.LocalDateTime

abstract class DucksAnalytics {

    private val file = File("ducks-analytics/" + getFilePath())

    init {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun log(text: String) {
        val date = LocalDateTime.now()
        file.appendText("$date: $text")
        file.appendText("\n")
    }

    abstract fun getFilePath(): String
}
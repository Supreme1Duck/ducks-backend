package com.ducks.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*

fun Application.installSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
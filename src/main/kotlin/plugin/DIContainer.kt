package com.ducks.plugin

import com.ducks.di.baseModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger


fun Application.installDI() {
    install(Koin) {
        SLF4JLogger() // Включает логирование Koin
        modules(baseModule())
    }
}
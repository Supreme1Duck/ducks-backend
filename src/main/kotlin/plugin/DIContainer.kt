package com.ducks.plugin

import com.ducks.di.baseModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger


fun Application.installDI() {
    val baseUrl = environment.config.property("app.baseUrl").getString()
    val photoroomApiKey = environment.config.property("photoroom.apiKey").getString()
    install(Koin) {
        SLF4JLogger() // Включает логирование Koin
        modules(baseModule(baseUrl, photoroomApiKey))
    }
}
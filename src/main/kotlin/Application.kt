package com.ducks

import com.ducks.database.installDB
import com.ducks.plugin.installAuth
import com.ducks.plugin.installDI
import com.ducks.plugin.installSerialization
import com.ducks.routings.configureRouting
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
// Используется в application.yaml для старта сервера
fun Application.module() {

    installSerialization()
    installDI()
    installAuth()
    installDB()

    configureRouting()
}
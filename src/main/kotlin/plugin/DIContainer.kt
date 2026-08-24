package com.ducks.plugin

import com.ducks.common.storage.S3Config
import com.ducks.di.baseModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger


fun Application.installDI() {
    val photoroomApiKey = environment.config.property("photoroom.apiKey").getString()
    val s3Config = readS3Config()

    install(Koin) {
        SLF4JLogger() // Включает логирование Koin
        modules(baseModule(photoroomApiKey, s3Config))
    }
}

private fun Application.readS3Config(): S3Config {
    val config = environment.config

    return S3Config(
        endpoint = config.property("s3.endpoint").getString(),
        region = config.property("s3.region").getString(),
        bucket = config.property("s3.bucket").getString(),
        accessKey = config.property("s3.accessKey").getString(),
        secretKey = config.property("s3.secretKey").getString(),
        publicBaseUrl = config.property("s3.publicBaseUrl").getString(),
        // У hoster.by под капотом MinIO, а он объектные ACL не поддерживает: публичное
        // чтение выдаётся политикой бакета. Поэтому по умолчанию acl не шлём вовсе.
        // Включать имеет смысл только на провайдере, где ACL реально работают.
        publicReadAcl = config.propertyOrNull("s3.publicReadAcl")
            ?.getString()
            ?.toBooleanStrictOrNull()
            ?: false,
    )
}

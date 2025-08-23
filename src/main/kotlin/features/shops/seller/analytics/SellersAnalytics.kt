package com.ducks.features.shops.seller.analytics

import java.io.File
import java.time.LocalDate

class SellersAnalytics {

    private val file = File("shops/sellers_analytics")
    private val suspiciousFile = File("shops/sellers_suspicious_analytics")

    fun logException(e: Exception) {
        file.writeText("${LocalDate.now()} - ${e.printStackTrace()}")
    }

    fun logSuspiciousSaveFiles(
        shopId: Long,
        fileName: String,
    ) {
       suspiciousFile.writeText("Подозрительный файл $fileName, ${LocalDate.now()}, от магазина с id $shopId /n /n")
    }

    fun logSuspiciousDeleteFile(
        shopId: Long,
        fileName: String,
    ) {
        suspiciousFile.writeText("${LocalDate.now()}: Файл $fileName не имеющийся в магазине с id $shopId, использован в попытке удаления /n /n")
    }
}
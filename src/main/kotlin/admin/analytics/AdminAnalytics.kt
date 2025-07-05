package com.ducks.admin.analytics

import com.ducks.common.DucksAnalytics

class AdminAnalytics: DucksAnalytics() {

    fun logException(e: Exception) {
        log(e.message.orEmpty())
    }

    override fun getFilePath(): String {
        return "admin/admin_analytics.txt"
    }
}
package com.duoji.app.data.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun formatDateTime(millis: Long): String {
        if (millis <= 0) return ""
        val ldt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return ldt.format(FORMATTER)
    }
}

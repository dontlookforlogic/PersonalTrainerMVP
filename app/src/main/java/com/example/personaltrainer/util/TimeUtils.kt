package com.example.personaltrainer.util

import java.time.*
import java.time.format.DateTimeFormatter

object TimeUtils {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun nowEpochMs(): Long = System.currentTimeMillis()

    fun formatEpochMs(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dt = Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDateTime()
        return dt.format(dateTimeFormatter)
    }

    fun startOfDayEpochMs(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun todayDayMask(date: LocalDate = LocalDate.now()): Int {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 8
            DayOfWeek.FRIDAY -> 16
            DayOfWeek.SATURDAY -> 32
            DayOfWeek.SUNDAY -> 64
        }
    }
}

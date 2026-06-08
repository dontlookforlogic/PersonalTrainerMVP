package com.example.personaltrainer.shared

object SharedWorkoutUtils {

    fun hasWorkoutOnDay(daysMask: Int, dayMask: Int): Boolean {
        return daysMask and dayMask != 0
    }

    fun isValidDaysMask(daysMask: Int): Boolean {
        return daysMask in 0..127
    }

    fun selectedDaysCount(daysMask: Int): Int {
        var count = 0

        val days = listOf(1, 2, 4, 8, 16, 32, 64)

        days.forEach { day ->
            if (daysMask and day != 0) {
                count++
            }
        }

        return count
    }
}
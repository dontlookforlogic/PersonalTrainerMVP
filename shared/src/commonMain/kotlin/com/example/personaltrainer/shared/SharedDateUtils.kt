package com.example.personaltrainer.shared

object SharedDateUtils {

    fun formatDurationShort(seconds: Long): String {
        val minutes = seconds / 60
        val sec = seconds % 60

        return "${minutes}м ${sec}с"
    }

    fun formatDurationFull(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val sec = seconds % 60

        return if (hours > 0) {
            "%dч %02dм %02dс".format(hours, minutes, sec)
        } else {
            "%dм %02dс".format(minutes, sec)
        }
    }
}
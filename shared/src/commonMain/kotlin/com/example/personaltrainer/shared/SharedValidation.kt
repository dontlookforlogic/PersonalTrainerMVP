package com.example.personaltrainer.shared

object SharedValidation {

    fun isEmailValid(email: String): Boolean {
        val value = email.trim()

        return value.contains("@") &&
                value.contains(".") &&
                value.length >= 5
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    fun isWorkoutTitleValid(title: String): Boolean {
        return title.trim().length >= 2
    }

    fun isExerciseTitleValid(title: String): Boolean {
        return title.trim().length >= 2
    }
}
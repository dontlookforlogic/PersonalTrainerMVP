package com.example.personaltrainer

import android.app.Application
import com.example.personaltrainer.data.AppDatabase

class PersonalTrainerApp : Application() {
    val db by lazy { AppDatabase.get(this) }
}

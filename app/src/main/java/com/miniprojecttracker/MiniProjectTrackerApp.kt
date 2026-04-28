package com.miniprojecttracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MiniProjectTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize things here if needed (e.g., Firebase is auto-initialized usually)
    }
}

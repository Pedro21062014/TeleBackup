package com.telebackup.app

import android.app.Application
import com.telebackup.app.data.PreferencesRepository

class TeleBackupApp : Application() {
    lateinit var preferences: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesRepository(this)
    }
}

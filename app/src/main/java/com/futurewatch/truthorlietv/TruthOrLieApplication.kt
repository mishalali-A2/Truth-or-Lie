package com.futurewatch.truthorlietv

import android.app.Application
import android.content.SharedPreferences

class TruthOrLieApplication : Application() {
//as global obj for music inst
    companion object {
        lateinit var instance: TruthOrLieApplication
            private set
        lateinit var prefs: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        MusicManager.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        MusicManager.onAppDestroy()
    }
}
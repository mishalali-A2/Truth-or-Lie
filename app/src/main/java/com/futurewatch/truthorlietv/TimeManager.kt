package com.futurewatch.truthorlietv

import android.content.Context
import android.content.SharedPreferences

object TimerManager {
    private const val DEFAULT_TIMER_SECONDS = 20
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    fun getTimerSeconds(): Int {
        return prefs?.getInt("timer_seconds", DEFAULT_TIMER_SECONDS) ?: DEFAULT_TIMER_SECONDS
    }

    fun getTimerMillis(): Long {
        return getTimerSeconds() * 1000L
    }

}
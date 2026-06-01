package com.futurewatch.truthorlietv

import android.content.Context

object CategoryManager {

    private val freeCategories = setOf(
        "general_knowledge",
        "science",
        "animals"
    )

    private val unlockedForSession = mutableSetOf<String>()
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun isUnlocked(category: String): Boolean {
        if (category in freeCategories || category in unlockedForSession) {
            return true
        }
        
        // Check for 24-hour purchase unlock
        appContext?.let { context ->
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val isUnlocked24h = prefs.getBoolean("category_${category}_unlocked_24h", false)
            if (isUnlocked24h) {
                val unlockTime = prefs.getLong("category_${category}_unlock_time", 0L)
                val currentTime = System.currentTimeMillis()
                val elapsedHours = (currentTime - unlockTime) / (1000 * 60 * 60)
                
                if (elapsedHours < 24) {
                    return true
                } else {
                    prefs.edit()
                        .remove("category_${category}_unlocked_24h")
                        .remove("category_${category}_unlock_time")
                        .apply()
                }
            }
        }
        
        return false
    }

    fun unlockTemporarily(category: String) {
        unlockedForSession.add(category)
    }

    fun resetSession() {
        unlockedForSession.clear()
    }
}
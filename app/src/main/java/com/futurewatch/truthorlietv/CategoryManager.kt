package com.futurewatch.truthorlietv

object CategoryManager {

    private val freeCategories = setOf(
        "general_knowledge",
        "science",
        "animals"
    )

    private val unlockedForSession = mutableSetOf<String>()

    fun isUnlocked(category: String): Boolean {
        return category in freeCategories || category in unlockedForSession
    }

    fun unlockTemporarily(category: String) {
        unlockedForSession.add(category)
    }

    fun resetSession() {
        unlockedForSession.clear()
    }
}
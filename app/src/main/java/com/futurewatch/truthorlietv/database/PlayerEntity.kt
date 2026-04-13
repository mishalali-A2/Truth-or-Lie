package com.futurewatch.truthorlietv.database

data class PlayerEntity(
    val id: Long = 0,
    val name: String,
    val points: Int,
    val timestamp: Long = System.currentTimeMillis()
)

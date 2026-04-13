package com.futurewatch.truthorlietv.database

import android.content.Context
import android.util.Log

class PlayerRepository(context: Context) {
    
    private val databaseHelper = LeaderboardDatabaseHelper(context)
    
    fun insertPlayer(name: String, points: Int) {
        Log.d("PlayerRepository", "Inserting player: $name with $points points")
        databaseHelper.insertOrUpdatePlayer(name, points)
    }
    
    fun updatePlayerScore(name: String, points: Int) {
        Log.d("PlayerRepository", "Updating player score: $name to $points points")
        databaseHelper.insertOrUpdatePlayer(name, points)
    }
    
    fun getAllPlayersSorted(): List<PlayerEntity> {
        val players = databaseHelper.getAllPlayersSorted()
        Log.d("PlayerRepository", "Retrieved ${players.size} players from database: ${players.map { "${it.name}: ${it.points}" }}")
        return players
    }
    
    fun getPlayerByName(name: String): PlayerEntity? {
        return databaseHelper.getPlayerByName(name)
    }
    
    fun deletePlayer(name: String) {
        // Note: This would require adding a delete method to the helper
        // For now, we'll just update the score to 0
        databaseHelper.insertOrUpdatePlayer(name, 0)
    }
    
    fun deleteAllPlayers() {
        Log.d("PlayerRepository", "Deleting all players from database")
        databaseHelper.deleteAllPlayers()
    }
    
    fun getPlayerRank(name: String): Int {
        return databaseHelper.getPlayerRank(name)
    }
}

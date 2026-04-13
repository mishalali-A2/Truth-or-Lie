package com.futurewatch.truthorlietv.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class LeaderboardDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "leaderboard.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_PLAYERS = "players"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_POINTS = "points"
        const val COLUMN_TIMESTAMP = "timestamp"    //in case of tie
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_PLAYERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_POINTS INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYERS")
        onCreate(db)
    }

    fun insertOrUpdatePlayer(name: String, points: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_POINTS, points)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }

        val cursor = db.query(
            TABLE_PLAYERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_NAME = ?",
            arrayOf(name),
            null, null, null
        )

        val result = if (cursor.moveToFirst()) {
            // Update existing player
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            Log.d("LeaderboardDatabase", "Updating existing player $name (ID: $id) with new score $points")
            db.update(TABLE_PLAYERS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
            id
        } else {
            //  new player
            Log.d("LeaderboardDatabase", "Inserting new player $name with score $points")
            db.insert(TABLE_PLAYERS, null, values)
        }

        cursor.close()
        return result
    }

    fun getAllPlayersSorted(): List<PlayerEntity> {
        val db = readableDatabase
        val players = mutableListOf<PlayerEntity>()

        val cursor = db.query(
            TABLE_PLAYERS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_POINTS DESC, $COLUMN_TIMESTAMP DESC"
        )

        Log.d("LeaderboardDatabase", "Querying all players, cursor count: ${cursor.count}")
        while (cursor.moveToNext()) {
            val player = PlayerEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                points = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POINTS)),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            )
            players.add(player)
            Log.d("LeaderboardDatabase", "Retrieved player: ${player.name} with ${player.points} points")
        }

        cursor.close()
        Log.d("LeaderboardDatabase", "Total players retrieved: ${players.size}")
        return players
    }

    fun getPlayerByName(name: String): PlayerEntity? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PLAYERS,
            null,
            "$COLUMN_NAME = ?",
            arrayOf(name),
            null, null, null
        )

        val player = if (cursor.moveToFirst()) {
            PlayerEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                points = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POINTS)),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            )
        } else {
            null
        }

        cursor.close()
        return player
    }

    // Delete all players
    fun deleteAllPlayers() {
        val db = writableDatabase
        db.delete(TABLE_PLAYERS, null, null)
    }

    fun getPlayerRank(name: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PLAYERS WHERE $COLUMN_POINTS > (SELECT $COLUMN_POINTS FROM $TABLE_PLAYERS WHERE $COLUMN_NAME = ?)",
            arrayOf(name)
        )

        val rank = if (cursor.moveToFirst()) {
            cursor.getInt(0) + 1 //top as 0 hence +1
        } else {
            0
        }

        cursor.close()
        return rank
    }
}

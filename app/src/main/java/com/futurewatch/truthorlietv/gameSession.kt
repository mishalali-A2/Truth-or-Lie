package com.futurewatch.truthorlietv

data class Player(
    var name: String,
    var score: Int = 0,
    var lastAnswer: Boolean? = null
)
object GameSession {

    var category: String = "science"
    var totalRounds: Int = 5
    var playerCount: Int = 2

    var currRound: Int = 1
    var currPlayerTurn: Int = 0

    var players: MutableList<Player> = mutableListOf()

    fun getCurrentPlayer(): Player {
        if (players.isEmpty()) {
            throw IllegalStateException("GameSession.players is empty! Cannot get current player.")
        }
        return players[currPlayerTurn % players.size]
    }

    fun getActualRound(): Int {
        if (players.isEmpty()) return 1
        return (currPlayerTurn / players.size) + 1
    }
    fun reset() {
        currRound = 1
        currPlayerTurn = 0
        players.forEach { it.score = 0 }
    }
}
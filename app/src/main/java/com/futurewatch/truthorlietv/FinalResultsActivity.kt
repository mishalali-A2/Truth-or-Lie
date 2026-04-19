package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import nl.dionsegijn.konfetti.xml.KonfettiView
import com.futurewatch.truthorlietv.database.PlayerRepository
import kotlin.concurrent.thread
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.models.Size

class FinalResultsActivity : AppCompatActivity() {

    private lateinit var tvWinnerName: TextView
    private lateinit var tvWinnerPoints: TextView
    private lateinit var leaderboardContainer: LinearLayout
    private lateinit var btnPlayAgain: Button
    private lateinit var konfettiView: KonfettiView
    private lateinit var playerRepository: PlayerRepository

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.final_results)

        MusicManager.resumeMusic()
        if (AdManager.isInterstitialReady()) {
            AdManager.showInterstitial(
                activity = this,
                onComplete = {
                    Log.d("FinalResults", "Interstitial completed")
                },
                onFailed = {
                    Log.d("FinalResults", "Interstitial not available, continuing")
                }
            )
        }

        tvWinnerName = findViewById(R.id.tvWinnerName)
        tvWinnerPoints = findViewById(R.id.tvWinnerPoints)
        leaderboardContainer = findViewById(R.id.leaderboardContainer)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        konfettiView = findViewById(R.id.konfettiView)
        playerRepository = PlayerRepository(this)

        saveScoresToDatabase()
        showResults()
        startContinuousConfetti()

        btnPlayAgain.setOnClickListener {
            //konfettiView.stop(party = Party())
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnPlayAgain.requestFocus()
    }

    private fun showResults() {
        val sortedPlayers = GameSession.players.sortedByDescending { it.score }
        val winner = sortedPlayers.first()

        tvWinnerName.text = winner.name
        tvWinnerPoints.text = "${winner.score} pts"

        leaderboardContainer.removeAllViews()

        sortedPlayers.forEachIndexed { index, player ->

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.setPadding(0, 20, 0, 20)

            val left = TextView(this)
            left.text = "#${index + 1}  ${player.name}"
            left.textSize = 20f
            left.setTextColor(
                if (index == 0) Color.parseColor("#FFA500")
                else Color.parseColor("#AAAAAA")
            )
            left.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val right = TextView(this)
            right.text = "${player.score}"
            right.textSize = 20f
            right.setTextColor(Color.parseColor("#7F3FFF"))

            row.addView(left)
            row.addView(right)

            leaderboardContainer.addView(row)
        }
    }

    private fun saveScoresToDatabase() {
        Log.d("FinalResultsActivity", "Saving ${GameSession.players.size} players to database: ${GameSession.players.map { "${it.name}: ${it.score}" }}")

        thread {
            try {
                GameSession.players.forEach { player ->
                    Log.d("FinalResultsActivity", "Saving player: ${player.name} with score ${player.score}")
                    playerRepository.updatePlayerScore(player.name, player.score)
                }
                Log.d("FinalResultsActivity", "✓ Finished saving all ${GameSession.players.size} players to database successfully")
            } catch (e: Exception) {
                Log.e("FinalResultsActivity", "✗ Error saving scores to database", e)
                e.printStackTrace()
            }
        }.join()
    }

    private fun startContinuousConfetti() {
        konfettiView.post {
            // Create multiple parties for a continuous, spread-out effect

            // Main party - full screen width continuous flow
            val mainParty = Party(
                speed = 2f,
                maxSpeed = 8f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(
                    Color.YELLOW,
                    Color.GREEN,
                    Color.MAGENTA,
                    Color.BLUE,
                    Color.CYAN,
                    Color.RED,
                    Color.parseColor("#FFA500"),
                    Color.parseColor("#FF69B4"),
                    Color.parseColor("#00CED1"),
                    Color.parseColor("#9370DB")
                ),
                size = listOf(Size(12), Size(15), Size(18), Size(20)),
                emitter = Emitter(duration = 15000).perSecond(120),
                position = Position.Relative(0.5, 0.0)
            )
            konfettiView.start(mainParty)

            // Left side wave
            val leftParty = Party(
                speed = 1.5f,
                maxSpeed = 7f,
                damping = 0.85f,
                spread = 300,
                colors = listOf(
                    Color.YELLOW,
                    Color.GREEN,
                    Color.BLUE,
                    Color.parseColor("#FFA500")
                ),
                size = listOf(Size(10), Size(14), Size(16)),
                emitter = Emitter(duration = 12000).perSecond(90),
                position = Position.Relative(0.2, 0.0)
            )
            konfettiView.start(leftParty)

            // Right side wave
            val rightParty = Party(
                speed = 1.8f,
                maxSpeed = 7.5f,
                damping = 0.88f,
                spread = 300,
                colors = listOf(
                    Color.MAGENTA,
                    Color.CYAN,
                    Color.RED,
                    Color.parseColor("#FF69B4")
                ),
                size = listOf(Size(10), Size(14), Size(16)),
                emitter = Emitter(duration = 12000).perSecond(90),
                position = Position.Relative(0.8, 0.0)
            )
            konfettiView.start(rightParty)
        }
    }
}
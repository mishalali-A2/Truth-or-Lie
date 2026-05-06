package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.final_results)

        MusicManager.resumeMusic()
        if (AdManager.isInterstitialReady()) {
            AdManager.showInterstitial(
                activity = this,
                onComplete = { Log.d("FinalResults", "Interstitial completed") },
                onFailed = { Log.d("FinalResults", "Interstitial not available") }
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
        val tvTie = findViewById<TextView>(R.id.tvTie)
        val tvWinnerLabel = findViewById<TextView>(R.id.tvWinnerLabel)
        val tvWinnerName = findViewById<TextView>(R.id.tvWinnerName)
        val tvWinnerPoints = findViewById<TextView>(R.id.tvWinnerPoints)
        val topScore = winner.score
        val tiedPlayers = sortedPlayers.filter { it.score == topScore }
        val isTie = tiedPlayers.size > 1
        if (isTie) {
            val names = tiedPlayers.joinToString(", ") { it.name }
            tvTie.visibility = View.VISIBLE
            tvTie.text = "It's a TIE!\n($names)"
            tvWinnerLabel.visibility = View.GONE
            tvWinnerName.visibility = View.GONE
            tvWinnerPoints.visibility = View.GONE
        } else {
            tvTie.visibility = View.GONE
            tvWinnerLabel.visibility = View.VISIBLE
            tvWinnerName.visibility = View.VISIBLE
            tvWinnerPoints.visibility = View.VISIBLE
            tvWinnerName.text = winner.name
            tvWinnerPoints.text = "${winner.score} pts"
        }
        leaderboardContainer.removeAllViews()
        sortedPlayers.forEachIndexed { index, player ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.setPadding(0, 16, 0, 16)
            row.gravity = android.view.Gravity.CENTER

            val left = TextView(this)
            left.text = "#${index + 1}  ${player.name}"
            left.textSize = 18f
            // Color logic: if tie, all tied players get orange, else only winner gets orange
            if (isTie && player.score == topScore) {
                left.setTextColor(Color.parseColor("#FFA500"))
            } else if (!isTie && index == 0) {
                left.setTextColor(Color.parseColor("#FFA500"))
            } else {
                left.setTextColor(Color.parseColor("#AAAAAA"))
            }

            val right = TextView(this)
            right.text = "             ${player.score}"
            right.textSize = 18f
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
            val party = Party(
                speed = 1.5f,
                maxSpeed = 5f,
                damping = 0.95f,
                spread = 360,
                colors = listOf(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE, Color.RED),
                size = listOf(Size(12), Size(20)),
                emitter = Emitter(duration = 15000).perSecond(20),
                position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
            )
            
            konfettiView.start(party)
        }
    }
}
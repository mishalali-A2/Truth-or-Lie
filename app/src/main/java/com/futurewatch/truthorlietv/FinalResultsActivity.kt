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
import com.futurewatch.truthorlietv.analytics.AdAnalyticsTracker
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.ScreenTracker

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

        ScreenTracker.attach(this, AnalyticsScreens.FINAL_RESULTS, previousScreen = AnalyticsScreens.RESULTS)
        logGameCompleted()

        // AdManager preloads/shows are unconditional here per existing behavior; we only wrap
        // with analytics (requested/started/completed/failed), never alter control flow.
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_REQUESTED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL,
                AnalyticsParams.AD_FORMAT to AdAnalyticsTracker.FORMAT_INTERSTITIAL
            )
        )
        if (AdManager.isInterstitialReady()) {
            AdManager.showInterstitial(
                activity = this,
                onComplete = {
                    Log.d("FinalResults", "Interstitial completed")
                    AdAnalyticsTracker.logShowCompleted(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, "COMPLETED")
                },
                onFailed = {
                    Log.d("FinalResults", "Interstitial not available")
                    AdAnalyticsTracker.logShowFailed(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, null, "not_available")
                }
            )
        } else {
            AnalyticsService.logEvent(
                AnalyticsEvents.AD_SHOW_SKIPPED_COOLDOWN,
                mapOf(
                    AnalyticsParams.AD_PLACEMENT to AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL,
                    AnalyticsParams.AD_FORMAT to AdAnalyticsTracker.FORMAT_INTERSTITIAL,
                    AnalyticsParams.AD_PRELOADED to false
                )
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
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.FINAL_RESULTS, AnalyticsParams.CONTROL_ID to "final_results_play_again")
            )
            //konfettiView.stop(party = Party())
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnPlayAgain.requestFocus()
    }

    /** category/round_count/player_count/is_tie/winner_score_bucket only — NEVER player names. */
    private fun logGameCompleted() {
        if (GameSession.players.isEmpty()) return
        val sortedPlayers = GameSession.players.sortedByDescending { it.score }
        val topScore = sortedPlayers.first().score
        val isTie = sortedPlayers.count { it.score == topScore } > 1

        AnalyticsService.logEvent(
            AnalyticsEvents.GAME_COMPLETED,
            mapOf(
                AnalyticsParams.CATEGORY_ID to GameSession.category,
                AnalyticsParams.ROUND_COUNT to GameSession.totalRounds,
                AnalyticsParams.PLAYER_COUNT to GameSession.players.size,
                AnalyticsParams.IS_TIE to isTie,
                AnalyticsParams.SCORE_BUCKET to scoreBucket(topScore)
            )
        )
    }

    /** Buckets the raw numeric score into a low-cardinality range string for BigQuery-friendly grouping. */
    private fun scoreBucket(score: Int): String = when {
        score <= 0 -> "0"
        score <= 200 -> "1-200"
        score <= 500 -> "201-500"
        score <= 1000 -> "501-1000"
        else -> "1000+"
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
                // Sanitized category only — never the raw exception message.
                AnalyticsService.logEvent(
                    AnalyticsEvents.ERROR_DATA_LOAD,
                    mapOf(
                        AnalyticsParams.ERROR_CATEGORY to "data_write_failed",
                        AnalyticsParams.ERROR_SOURCE to "final_results_save_scores"
                    )
                )
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
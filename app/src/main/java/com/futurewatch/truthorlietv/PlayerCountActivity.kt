package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class PlayerCountActivity : AppCompatActivity() {

    private var playerCount = 1

    private val MIN_PLAYERS = 1
    private val MAX_PLAYERS = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.player_count)

        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.PLAYER_COUNT, previousScreen = AnalyticsScreens.ROUNDS)

        val main = findViewById<android.view.View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnMinus = findViewById<ImageButton>(R.id.btnMinus)
        val btnPlus = findViewById<ImageButton>(R.id.btnPlus)
        val txtPlayerCount = findViewById<TextView>(R.id.txtPlayerCount)
        val btnPlayers = findViewById<Button>(R.id.btnPlayers)

        // def
        txtPlayerCount.text = playerCount.toString()

        btnMinus.setOnClickListener {
            if (playerCount > MIN_PLAYERS) {
                playerCount--
                txtPlayerCount.text = playerCount.toString()
                AnalyticsService.logEvent(
                    AnalyticsEvents.CONTROL_CLICK,
                    mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.PLAYER_COUNT, AnalyticsParams.CONTROL_ID to "player_count_minus")
                )
            }
        }

        btnPlus.setOnClickListener {
            if (playerCount < MAX_PLAYERS) {
                playerCount++
                txtPlayerCount.text = playerCount.toString()
                AnalyticsService.logEvent(
                    AnalyticsEvents.CONTROL_CLICK,
                    mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.PLAYER_COUNT, AnalyticsParams.CONTROL_ID to "player_count_plus")
                )
            }
        }
//set name screen
        btnPlayers.setOnClickListener {
            GameSession.playerCount = playerCount
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.PLAYER_COUNT, AnalyticsParams.CONTROL_ID to "player_count_continue")
            )
            AnalyticsService.logEvent(
                AnalyticsEvents.PLAYER_COUNT_SELECTED,
                mapOf(AnalyticsParams.PLAYER_COUNT to playerCount)
            )
            val intent = Intent(this, PlayerNamesActivity::class.java)
            startActivity(intent)
        }
    }
}

package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.InputTracker
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class RoundsActivity : AppCompatActivity() {

    private var selectedRounds = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.rounds)

        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.ROUNDS, previousScreen = AnalyticsScreens.CATEGORIES)

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btn3 = findViewById<Button>(R.id.btn3)
        val btn5 = findViewById<Button>(R.id.btn5)
        val btn7 = findViewById<Button>(R.id.btn7)
        val btn10 = findViewById<Button>(R.id.btn10)
        val btn15 = findViewById<Button>(R.id.btn15)

        val buttons = listOf(btn3, btn5, btn7, btn10, btn15)

        //anim
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->

            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()
            }
        }

        buttons.forEach { button ->

            button.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                focusListener.onFocusChange(v, hasFocus)
                InputTracker.logFocusChanged(AnalyticsScreens.ROUNDS, "rounds_btn_${button.text}", hasFocus)
            }

            button.setOnClickListener {

                selectedRounds = button.text.toString().toInt()

                buttons.forEach { it.isSelected = false }
                button.isSelected = true

               GameSession.totalRounds = selectedRounds

                AnalyticsService.logEvent(
                    AnalyticsEvents.CONTROL_CLICK,
                    mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.ROUNDS, AnalyticsParams.CONTROL_ID to "rounds_btn_$selectedRounds")
                )
                AnalyticsService.logEvent(
                    AnalyticsEvents.ROUNDS_SELECTED,
                    mapOf(AnalyticsParams.ROUND_COUNT to selectedRounds)
                )

                val intent = Intent(this, PlayerCountActivity::class.java)
                startActivity(intent)
            }
        }

        // def
        btn5.requestFocus()
    }
}
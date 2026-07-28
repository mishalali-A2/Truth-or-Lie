package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class HowToPlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.how_to_play)

        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.HOW_TO_PLAY, previousScreen = AnalyticsScreens.MAIN)

        // Back to Menu Button
        val backBtn = findViewById<Button>(R.id.btnBacktoMenu)
        backBtn.isFocusable = true
        backBtn.isFocusableInTouchMode = true

        backBtn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(30f)
                    .setDuration(150)
                    .start()
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()

                v.setBackgroundResource(R.drawable.text_input)
            }
        }

        backBtn.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.HOW_TO_PLAY, AnalyticsParams.CONTROL_ID to "how_to_play_back")
            )
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
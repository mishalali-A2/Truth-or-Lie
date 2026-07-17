package com.futurewatch.truthorlietv

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Looper
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.InputTracker
import com.futurewatch.truthorlietv.analytics.ScreenTracker


class MainActivity : AppCompatActivity() {
    private var titleAnim: ObjectAnimator? = null
    private var subtitleAnim: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ScreenTracker.attach(this, AnalyticsScreens.MAIN, previousScreen = AnalyticsScreens.SPLASH)

        AdManager.initialize(this, testMode = false) {
            Log.d("MainActivity", "Unity Ads ready - Rewarded: ${AdManager.isRewardedReady()}, Interstitial: ${AdManager.isInterstitialReady()}")
        }

        Handler(Looper.getMainLooper()).postDelayed({
            MusicManager.startMusic()
            Log.d("MainActivity", "Music started")
        }, 100)


        val title = findViewById<View>(R.id.app_title)
        val subtitle= findViewById<View>(R.id.app_subtitle)


        // title anim -> loop
        fun createFloatAnim(view: View): ObjectAnimator {
            return ObjectAnimator.ofFloat(view, "translationY", -14f, 14f).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator() // smooth ease in/out
            }
        }

        titleAnim = createFloatAnim(title)
        subtitleAnim = createFloatAnim(subtitle)

        titleAnim?.start()
        subtitleAnim?.start()

        // btn anim
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->

            v.clearAnimation()

            if (hasFocus) {
                v.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
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
                v.translationZ = 0f
            }
            InputTracker.logFocusChanged(AnalyticsScreens.MAIN, (v.tag as? String) ?: v.id.toString(), hasFocus)
        }

        val startBtn = findViewById<Button>(R.id.btnStart)
        val howToPlayBtn = findViewById<Button>(R.id.btnHowToPlay)
        val leaderboardBtn = findViewById<Button>(R.id.btnLeaderboard)
        val settingsBtn = findViewById<Button>(R.id.btnSettings)

        startBtn.tag = "main_start"
        howToPlayBtn.tag = "main_how_to_play"
        leaderboardBtn.tag = "main_leaderboard"
        settingsBtn.tag = "main_settings"

        startBtn.onFocusChangeListener = focusListener
        howToPlayBtn.onFocusChangeListener = focusListener
        leaderboardBtn.onFocusChangeListener = focusListener
        settingsBtn.onFocusChangeListener = focusListener

        startBtn.setOnClickListener {
            AnalyticsService.logEvent(AnalyticsEvents.CONTROL_CLICK, mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.MAIN, AnalyticsParams.CONTROL_ID to "main_start"))
            startActivity(Intent(this, CategoriesActivity::class.java))
        }

        howToPlayBtn.setOnClickListener {
            AnalyticsService.logEvent(AnalyticsEvents.CONTROL_CLICK, mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.MAIN, AnalyticsParams.CONTROL_ID to "main_how_to_play"))
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        leaderboardBtn.setOnClickListener {
            AnalyticsService.logEvent(AnalyticsEvents.CONTROL_CLICK, mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.MAIN, AnalyticsParams.CONTROL_ID to "main_leaderboard"))
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        settingsBtn.setOnClickListener {
            AnalyticsService.logEvent(AnalyticsEvents.CONTROL_CLICK, mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.MAIN, AnalyticsParams.CONTROL_ID to "main_settings"))
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Default
        startBtn.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        titleAnim?.cancel()
        subtitleAnim?.cancel()
        MusicManager.pauseMusic()
    }
    override fun onResume() {
        super.onResume()
        titleAnim?.start()
        subtitleAnim?.start()
        MusicManager.startMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.stopMusic()
    }
}

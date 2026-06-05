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


class MainActivity : AppCompatActivity() {
    private var titleAnim: ObjectAnimator? = null
    private var subtitleAnim: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        }

        val startBtn = findViewById<Button>(R.id.btnStart)
        val howToPlayBtn = findViewById<Button>(R.id.btnHowToPlay)
        val leaderboardBtn = findViewById<Button>(R.id.btnLeaderboard)
        val settingsBtn = findViewById<Button>(R.id.btnSettings)

        startBtn.onFocusChangeListener = focusListener
        howToPlayBtn.onFocusChangeListener = focusListener
        leaderboardBtn.onFocusChangeListener = focusListener
        settingsBtn.onFocusChangeListener = focusListener

        startBtn.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }

        howToPlayBtn.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }
        leaderboardBtn.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        settingsBtn.setOnClickListener {
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

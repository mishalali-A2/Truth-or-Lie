package com.futurewatch.truthorlietv

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Looper
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button


class MainActivity : AppCompatActivity(), InfaticaConsentDialog.ConsentListener {
    private var titleAnim: ObjectAnimator? = null
    private var subtitleAnim: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(this, testMode = true) {
            Log.d("MainActivity", "Unity Ads ready - Rewarded: ${AdManager.isRewardedReady()}, Interstitial: ${AdManager.isInterstitialReady()}")
        }

        setContentView(R.layout.activity_main)

        checkAndShowInfaticaConsent()
        Handler(Looper.getMainLooper()).postDelayed({
            MusicManager.startMusic()
            Log.d("MainActivity", "Music started")
        }, 100)


        val title = findViewById<View>(R.id.app_title)
        val subtitle= findViewById<View>(R.id.app_subtitle)


        // title anim -> loop
        fun createFloatAnim(view: View): ObjectAnimator {
            return ObjectAnimator.ofFloat(view, "translationY", -10f, 10f).apply {
                duration = 2500
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator() // Smooth linear movement
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
                    .scaleX(1.08f)
                    .scaleY(1.08f)
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
    private fun checkAndShowInfaticaConsent() {
        if (InfaticaConsentDialog.shouldShowConsent(this)) {
            val consentDialog = InfaticaConsentDialog()
            consentDialog.show(supportFragmentManager, "InfaticaConsentDialog")
        } else if (InfaticaConsentDialog.hasAccepted(this)) {
            val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
            val manuallyDisabled = prefs.getBoolean("network_sdk_manually_disabled", false)

            if (!manuallyDisabled) {
                prefs.edit().putBoolean("network_sdk_enabled", true).apply()
                InfaticaConsentDialog.updateNetworkSdkToggle(this, true)
                InfaticaManager.saveConsent(this, true)
                InfaticaManager.setEnabled(this, true)
            } else {
                InfaticaConsentDialog.updateNetworkSdkToggle(this, false)
            }
        }else{
            InfaticaConsentDialog.updateNetworkSdkToggle(this, false)
        }
    }

    override fun onConsentAccepted() {
        Log.d("MainActivity", "Infatica consent accepted - toggle turned ON")
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("network_sdk_enabled", true)
            .putBoolean("network_sdk_manually_disabled", false)
            .apply()
        InfaticaManager.saveConsent(this, true)
        InfaticaManager.setEnabled(this, true)
    }

    override fun onConsentDeclined() {
        Log.d("MainActivity", "Infatica consent declined - toggle turned OFF")
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("network_sdk_enabled", false)
            .apply()
        InfaticaManager.saveConsent(this, false)
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
        MusicManager.resumeMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.stopMusic()
    }
}

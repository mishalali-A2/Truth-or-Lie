package com.futurewatch.truthorlietv

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.os.Looper
import android.os.Handler


class SettingsActivity : AppCompatActivity() {

    private lateinit var switchMusic: SwitchCompat
    private lateinit var backBtn: Button
    private lateinit var musicChips: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        MusicManager.resumeMusic()

        switchMusic = findViewById(R.id.switchMusic)
        backBtn = findViewById(R.id.btnBack)


        musicChips = listOf(
            findViewById(R.id.chipFunkyGroove) ?: findChipByText("Party Vibes"),
            findViewById(R.id.chipChillLounge) ?: findChipByText("Chill Lounge"),
            findViewById(R.id.chipRetroArcade) ?: findChipByText("Retro Arcade"),
            findViewById(R.id.chipPartyVibes) ?: findChipByText("Epic Adventure"),
            findViewById(R.id.chipEpicAdventure) ?: findChipByText("Funky Groove")
        ).filterNotNull()

        switchMusic.isChecked = MusicManager.isEnabled()
        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            MusicManager.setEnabled(isChecked)
            val message = if (isChecked) "Music enabled" else "Music disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        setupMusicGenreSelection()
        setupOtherSettings()
        setupFocusAnimation()
        addInfaticaStatus()
        //for billing debugging n testing
        addDebugPanel()

        backBtn.setOnClickListener {
            finish()
        }

        switchMusic.requestFocus()
    }

    private fun setupMusicGenreSelection() {
        val currentGenre = MusicManager.getCurrentGenre()

        val musicContainer = findViewById<View>(R.id.musicContainer) ?: return

        val allButtons = getAllButtonsInContainer(musicContainer)

        allButtons.forEach { button ->
            if (button is Button) {
                val buttonText = button.text.toString()

                if (isMusicGenre(buttonText)) {
                    if (buttonText == currentGenre) {
                        button.setBackgroundResource(R.drawable.chip_selected)
                    } else {
                        button.setBackgroundResource(R.drawable.chip_bg)
                    }

                    button.setOnClickListener {
                        MusicManager.changeGenre(buttonText)
                        updateGenreSelection(buttonText)
                        Toast.makeText(this, "Music changed to: $buttonText", Toast.LENGTH_SHORT).show()
                    }

                    button.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150).start()
                            v.translationZ = 20f
                        } else {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                            v.translationZ = 0f
                        }
                    }
                }
            }
        }
    }

    private fun updateGenreSelection(selectedGenre: String) {
        val musicContainer = findViewById<View>(R.id.musicContainer) ?: return
        val allButtons = getAllButtonsInContainer(musicContainer)

        allButtons.forEach { button ->
            if (button is Button && isMusicGenre(button.text.toString())) {
                if (button.text.toString() == selectedGenre) {
                    button.setBackgroundResource(R.drawable.chip_selected)
                } else {
                    button.setBackgroundResource(R.drawable.chip_bg)
                }
            }
        }
    }

    private fun isMusicGenre(text: String): Boolean {
        return text in listOf("Party Vibes", "Chill Lounge", "Retro Arcade", "Epic Adventure", "Funky Groove")
    }

    private fun getAllButtonsInContainer(container: View): List<View> {
        val buttons = mutableListOf<View>()

        if (container is android.view.ViewGroup) {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is Button) {
                    buttons.add(child)
                } else if (child is android.view.ViewGroup) {
                    buttons.addAll(getAllButtonsInContainer(child))
                }
            }
        }

        return buttons
    }

    private fun findChipByText(text: String): Button? {
        val musicContainer = findViewById<View>(R.id.musicContainer)
        if (musicContainer != null) {
            return findButtonByText(musicContainer, text)
        }
        return null
    }

    private fun findButtonByText(container: View, targetText: String): Button? {
        if (container is Button && container.text.toString() == targetText) {
            return container
        }

        if (container is android.view.ViewGroup) {
            for (i in 0 until container.childCount) {
                val found = findButtonByText(container.getChildAt(i), targetText)
                if (found != null) return found
            }
        }

        return null
    }

    private fun setupOtherSettings() {
        setupTimerSelection()

        val switchNetwork = findViewById<SwitchCompat>(R.id.switchNetwork)
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        val isInfaticaEnabled = prefs.getBoolean("network_sdk_enabled", false)
        switchNetwork?.isChecked = isInfaticaEnabled

        switchNetwork?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check network connection first
                if (!InfaticaManager.hasNetworkConnection(this)) {
                    Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show()
                    switchNetwork?.isChecked = false
                    return@setOnCheckedChangeListener
                }
                // Show consent dialog before enabling
                showInfaticaConsentDialog()
            } else {
                // Disable Infatica
                prefs.edit().putBoolean("network_sdk_enabled", false).apply()
                InfaticaManager.setEnabled(this, false)
                Toast.makeText(this, "Network sharing disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Remove Ads
        setupSettingsItemFocus(R.id.removeAds, "Remove Ads")
        setupSettingsItemFocus(R.id.restorePurchases, "Restore Purchases")
        setupSettingsItemFocus(R.id.rateApp, "Rate App")
        setupSettingsItemFocus(R.id.termsPrivacy, "Terms & Privacy")
    }

    private fun setupSettingsItemFocus(itemId: Int, itemName: String) {
        val itemView = findViewById<View>(itemId)
        itemView?.apply {
            isFocusable = true
            isClickable = true

            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    // Highlight when focused
                    v.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .translationZ(20f)
                        .setDuration(150)
                        .start()
                    v.setBackgroundColor(Color.parseColor("#2a2a3e"))
                    // Also highlight the text
                    val textView = (v as? LinearLayout)?.findViewById<TextView>(android.R.id.text1)
                    textView?.setTextColor(Color.parseColor("#FFA500"))
                } else {
                    // Remove highlight when not focused
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .setDuration(150)
                        .start()
                    v.setBackgroundColor(Color.TRANSPARENT)
                    val textView = (v as? LinearLayout)?.findViewById<TextView>(android.R.id.text1)
                    textView?.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }

            setOnClickListener {
                when (itemId) {
                    R.id.removeAds -> Toast.makeText(this@SettingsActivity, "Remove Ads - Coming soon!", Toast.LENGTH_SHORT).show()
                    R.id.restorePurchases -> Toast.makeText(this@SettingsActivity, "Restore Purchases - Coming soon!", Toast.LENGTH_SHORT).show()
                    R.id.rateApp -> Toast.makeText(this@SettingsActivity, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                    R.id.termsPrivacy -> Toast.makeText(this@SettingsActivity, "Terms & Privacy - Coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showInfaticaConsentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Network Sharing")
            .setMessage("This allows sharing of idle network resources to support the app. You can disable this anytime in settings.\n\n" +
                    "• Works over WiFi or Ethernet\n" +
                    "• Uses minimal bandwidth\n" +
                    "• You remain in control")
            .setPositiveButton("Agree & Enable") { _, _ ->
                val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
                prefs.edit().putBoolean("network_sdk_enabled", true).apply()
                InfaticaManager.saveConsent(this, true)
                InfaticaManager.setEnabled(this, true)

                val switchNetwork = findViewById<SwitchCompat>(R.id.switchNetwork)
                switchNetwork?.isChecked = true

                Toast.makeText(this, "Network sharing enabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No, Thanks") { _, _ ->
                val switchNetwork = findViewById<SwitchCompat>(R.id.switchNetwork)
                switchNetwork?.isChecked = false
                Toast.makeText(this, "Network sharing disabled", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // Add a status TextView to your settings layout
    private fun addInfaticaStatus() {
        val statusText = TextView(this).apply {
            text = getInfaticaStatus()
            textSize = 14f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(40, 10, 40, 10)
            id = View.generateViewId()
        }

        // Add to your settings layout
        val parentLayout = findViewById<LinearLayout>(R.id.settingsRoot)
        parentLayout?.addView(statusText)

        // Update status periodically
        Handler(Looper.getMainLooper()).postDelayed({
            statusText.text = getInfaticaStatus()
        }, 2000)
    }

    private fun getInfaticaStatus(): String {
        return buildString {
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("📡 INFATICA STATUS\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("Network Connected: ${if (InfaticaManager.hasNetworkConnection(this@SettingsActivity)) "✅" else "❌"}\n")
            append("SDK Enabled: ${if (InfaticaManager.isEnabled(this@SettingsActivity)) "✅" else "❌"}\n")
            append("Service Running: ${if (InfaticaManager.isServiceRunning()) "✅" else "❌"}\n")
            val sdkId = InfaticaManager.getSdkId()
            if (sdkId.isNotEmpty()) {
                append("SDK ID: ${sdkId.take(16)}...\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    private fun setupTimerSelection() {
        val timer10 = findViewById<Button>(R.id.timer10)
        val timer20 = findViewById<Button>(R.id.timer20)
        val timer30 = findViewById<Button>(R.id.timer30)
        val timer40 = findViewById<Button>(R.id.timer40)
        val timer50 = findViewById<Button>(R.id.timer50)

        val timerButtons = listOf(timer10, timer20, timer30, timer40, timer50).filterNotNull()

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTimer = prefs.getInt("timer_seconds", 20)

        timerButtons.forEach { button ->
            val seconds = button.text.toString().toIntOrNull()
            if (seconds != null) {
                if (seconds == currentTimer) {
                    button.setBackgroundResource(R.drawable.chip_selected)
                } else {
                    button.setBackgroundResource(R.drawable.chip_bg)
                }

                button.setOnClickListener {
                    prefs.edit().putInt("timer_seconds", seconds).apply()
                    updateTimerSelection(seconds, timerButtons)
                    Toast.makeText(this, "Timer set to $seconds seconds", Toast.LENGTH_SHORT).show()
                }

                button.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                        v.translationZ = 20f
                    } else {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        v.translationZ = 0f
                    }
                }
            }
        }
    }

    private fun updateTimerSelection(selectedSeconds: Int, buttons: List<Button>) {
        buttons.forEach { button ->
            val seconds = button.text.toString().toIntOrNull()
            if (seconds != null) {
                if (seconds == selectedSeconds) {
                    button.setBackgroundResource(R.drawable.chip_selected)
                } else {
                    button.setBackgroundResource(R.drawable.chip_bg)
                }
            }
        }
    }
    private fun setupFocusAnimation() {
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                v.translationZ = 20f
            } else {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                v.translationZ = 0f
            }
        }

        backBtn.onFocusChangeListener = focusListener
        switchMusic.onFocusChangeListener = focusListener
    }

    private fun addDebugPanel() {
        // Find the settingsRoot LinearLayout directly
        val mainLayout = findViewById<LinearLayout>(R.id.settingsRoot)

        if (mainLayout != null) {
            addPanelToLayout(mainLayout)
        } else {
            // Fallback: try to find any LinearLayout that's a child of the content view
            val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(android.R.id.content)
            // If all else fails, just show a toast
            Toast.makeText(this, "Debug panel could not be added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPanelToLayout(parentLayout: LinearLayout) {
        // Create debug panel
        val debugPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.parseColor("#1a1a2e"))

            // Title
            val titleView = TextView(this@SettingsActivity).apply {
                text = "🔧 DEBUG BILLING PANEL"
                textSize = 20f
                setTextColor(Color.parseColor("#FFA500"))
                setPadding(0, 0, 0, 20)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            // Divider
            val divider = View(this@SettingsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                )
                setBackgroundColor(Color.parseColor("#333333"))
            }

            // Status display
            val statusText = TextView(this@SettingsActivity).apply {
                text = getDebugStatus()
                textSize = 14f
                setTextColor(Color.parseColor("#AAAAAA"))
                setPadding(0, 15, 0, 15)
            }

            // Button: Remove Ads
            val btnRemoveAds = Button(this@SettingsActivity).apply {
                text = "💰 Simulate: Remove Ads Purchase"
                setBackgroundColor(Color.parseColor("#4CAF50"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    val prefs = TruthOrLieApplication.prefs
                    prefs.edit().putBoolean("ads_removed", true).apply()
                    statusText.text = getDebugStatus()
                    Toast.makeText(this@SettingsActivity, "✅ Simulated: Ads Removed Permanently", Toast.LENGTH_LONG).show()
                }
            }

            // Button: Unlock Categories
            val btnUnlockCategories = Button(this@SettingsActivity).apply {
                text = "🎮 Simulate: Unlock All Categories"
                setBackgroundColor(Color.parseColor("#2196F3"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    val prefs = TruthOrLieApplication.prefs
                    prefs.edit().putBoolean("all_categories_unlocked", true).apply()
                    statusText.text = getDebugStatus()
                    Toast.makeText(this@SettingsActivity, "✅ Simulated: All Categories Unlocked", Toast.LENGTH_LONG).show()
                }
            }

            // Button: Premium Subscription
            val btnPremium = Button(this@SettingsActivity).apply {
                text = "⭐ Simulate: Premium Subscription"
                setBackgroundColor(Color.parseColor("#FF9800"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    val prefs = TruthOrLieApplication.prefs
                    prefs.edit().putBoolean("premium_access", true).apply()
                    statusText.text = getDebugStatus()
                    Toast.makeText(this@SettingsActivity, "✅ Simulated: Premium Subscription Active", Toast.LENGTH_LONG).show()
                }
            }

            // Button: Reset All
            val btnReset = Button(this@SettingsActivity).apply {
                text = "🗑️ Reset All Purchases"
                setBackgroundColor(Color.parseColor("#F44336"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    val prefs = TruthOrLieApplication.prefs
                    prefs.edit()
                        .remove("ads_removed")
                        .remove("all_categories_unlocked")
                        .remove("premium_access")
                        .apply()
                    statusText.text = getDebugStatus()
                    Toast.makeText(this@SettingsActivity, "✅ Reset: All debug purchases cleared", Toast.LENGTH_LONG).show()
                }
            }

            // Button: Test Interstitial
            val btnTestInterstitial = Button(this@SettingsActivity).apply {
                text = "📺 Test: Show Interstitial Ad"
                setBackgroundColor(Color.parseColor("#9C27B0"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    AdManager.showInterstitial(
                        activity = this@SettingsActivity,
                        onComplete = {
                            runOnUiThread {
                                Toast.makeText(this@SettingsActivity, "Ad completed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailed = {
                            runOnUiThread {
                                Toast.makeText(this@SettingsActivity, "Ad failed or not available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Add this to your debug panel buttons
            val btnTestInterstitialShow = Button(this@SettingsActivity).apply {
                text = "🎬 Test: Show Interstitial Ad (Force)"
                setBackgroundColor(Color.parseColor("#FF5722"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    Log.d("Settings", "Manual interstitial test triggered")
                    Toast.makeText(this@SettingsActivity, "Showing interstitial...", Toast.LENGTH_SHORT).show()

                    AdManager.showInterstitial(
                        activity = this@SettingsActivity,
                        onComplete = {
                            runOnUiThread {
                                Toast.makeText(this@SettingsActivity, "✅ Interstitial completed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailed = {
                            runOnUiThread {
                                Toast.makeText(this@SettingsActivity, "❌ Interstitial failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            // Button: Test Rewarded
            val btnTestRewarded = Button(this@SettingsActivity).apply {
                text = "🎁 Test: Show Rewarded Ad"
                setBackgroundColor(Color.parseColor("#E91E63"))
                setTextColor(Color.WHITE)
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
                setOnClickListener {
                    AdManager.showRewardedAd(
                        activity = this@SettingsActivity,
                        onRewardEarned = {
                            runOnUiThread {
                                Toast.makeText(this@SettingsActivity, "🎉 Reward earned!", Toast.LENGTH_LONG).show()
                            }
                        },
                        onFailed = {
                            runOnUiThread {
                                Toast.makeText(this@SettingsActivity, "Rewarded ad failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Add all views
            addView(titleView)
            addView(divider)
            addView(statusText)
            addView(btnRemoveAds)
            addView(btnUnlockCategories)
            addView(btnPremium)
            addView(btnReset)
            addView(btnTestInterstitial)
            addView(btnTestRewarded)
        }

        // Add panel to parent layout
        parentLayout.addView(debugPanel)
    }

    private fun getDebugStatus(): String {
        val prefs = TruthOrLieApplication.prefs
        val adsRemoved = prefs.getBoolean("ads_removed", false)
        val categoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)
        val premiumAccess = prefs.getBoolean("premium_access", false)

        return buildString {
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("📊 CURRENT DEBUG STATUS:\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append(if (adsRemoved) "✅ " else "❌ "); append("Ads Removed\n")
            append(if (categoriesUnlocked) "✅ " else "❌ "); append("All Categories Unlocked\n")
            append(if (premiumAccess) "✅ " else "❌ "); append("Premium Access\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("💡 Tap buttons above to simulate purchases")
        }
    }


    override fun onPause() {
        super.onPause()
        MusicManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
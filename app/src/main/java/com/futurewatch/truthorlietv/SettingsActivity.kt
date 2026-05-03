package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.futurewatch.truthorlietv.CategoryManager.resetSession


class SettingsActivity : AppCompatActivity() {

    private lateinit var switchMusic: SwitchCompat
    private lateinit var backBtn: Button
    private lateinit var musicChips: List<View>
    private var selectedAppPackage: String? = null
    private lateinit var musicContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        MusicManager.resumeMusic()

        switchMusic = findViewById(R.id.switchMusic)
        backBtn = findViewById(R.id.btnBack)
        musicContainer = findViewById(R.id.musicContainer)
        switchMusic.isChecked = MusicManager.isEnabled()
        musicContainer.visibility =
            if (switchMusic.isChecked) View.VISIBLE else View.GONE


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

            if (isChecked) {
                musicContainer.visibility = View.VISIBLE
                musicContainer.alpha = 0f
                musicContainer.animate().alpha(1f).setDuration(200).start()
            } else {
                musicContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        musicContainer.visibility = View.GONE
                    }
                    .start()
            }

            val message = if (isChecked) "Music enabled" else "Music disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        setupMusicGenreSelection()
        setupOtherSettings()
        setupFocusAnimation()
        setupOtherAppsSection()

        //for billing debugging n testing
        //addDebugPanel()

        resetSession()

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

       // val switchNetwork = findViewById<SwitchCompat>(R.id.switchNetwork)
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)


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
                    val textView = (v as? LinearLayout)?.findViewById<TextView>(android.R.id.text1)
                    textView?.setTextColor(Color.parseColor("#FFA500"))
                } else {
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
                    R.id.removeAds -> {
                        val intent = Intent(this@SettingsActivity, PurchaseActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.restorePurchases -> {
                        Toast.makeText(this@SettingsActivity, "Restoring all purchases...", Toast.LENGTH_SHORT).show()
                        TruthOrLieApplication.billingRepository.restorePurchases()
                    }
                    R.id.rateApp -> {
                        Toast.makeText(this@SettingsActivity, "Rating will be available once live!", Toast.LENGTH_SHORT).show()
                    }
                    R.id.termsPrivacy -> {
                        showTermsPrivacyDialog()
                    }
                }
            }
        }
    }

    private fun showTermsPrivacyDialog() {
        val options = arrayOf("Privacy Policy", "Terms of Service")
        AlertDialog.Builder(this, R.style.Theme_TruthorLieTV_Dialog)
            .setTitle("Terms & Privacy")
            .setItems(options) { _, which ->
                val url = if (which == 0) {
                    "https://infatica-sdk.io/uploads/privacy-policy.pdf"
                } else {
                    "https://futurewatch.co/terms"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }


    private fun setupOtherAppsSection() {
        val otherAppsContainer = findViewById<LinearLayout>(R.id.otherAppsContainer) ?: return

        otherAppsContainer.removeAllViews()

        val apps = listOf(
            "Heads Up To Go" to "com.futurewatch.headsuptogo",
            "World Clock TV" to "com.futurewatch.world_clock_tv",
            "Tranquil" to "com.tranquil.androidtv",
            "Minesweeper" to "com.futurewatch.minesweeper",
            "Turborg 2D-Racing" to "co.futurewatch.turborg2d.racing",
            "Moodscreen TV" to "com.moodscreen.tv"
        )

        val chipsPerRow = 3
        val rows = apps.chunked(chipsPerRow)

        rows.forEachIndexed { rowIndex, rowApps ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 15
                }
            }

            rowApps.forEachIndexed { index, (appName, packageName) ->
                val chipButton = Button(this).apply {
                    text = appName
                    tag=packageName
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.chip_bg)
                    setPadding(30, 20, 30, 20)

                    val params = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        if (index > 0) marginStart = 15
                    }
                    layoutParams = params

                    isFocusable = true
                    isClickable = true

                    // Focus animation matching music buttons
                    onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150).start()
                            v.translationZ = 20f
                            setBackgroundResource(R.drawable.chip_selected)
                        } else {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                            v.translationZ = 0f
                            setBackgroundResource(R.drawable.chip_bg)
                        }
                    }

                    setOnClickListener {
                        selectedAppPackage = packageName
                        updateOtherAppsSelection(otherAppsContainer, packageName)
                        openAppOrPlayStore(packageName)
                    }
                }
                rowLayout.addView(chipButton)
            }

            // Fill empty space if odd number of items
            if (rowApps.size < chipsPerRow) {
                val spacer = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                rowLayout.addView(spacer)
            }

            otherAppsContainer.addView(rowLayout)
        }
    }

    private fun openAppOrPlayStore(packageName: String) {
        try {
            //opening play stor
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to browser
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOtherAppsSelection(container: LinearLayout, selectedPackage: String) {
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue

            for (j in 0 until row.childCount) {
                val view = row.getChildAt(j)

                if (view is Button) {
                    val tagPackage = view.tag as? String

                    if (tagPackage == selectedPackage) {
                        view.setBackgroundResource(R.drawable.chip_selected)
                    } else {
                        view.setBackgroundResource(R.drawable.chip_bg)
                    }
                }
            }
        }
    }
    private val Int.sp: Float
        get() = this.toFloat() * resources.displayMetrics.scaledDensity

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
        val mainLayout = findViewById<LinearLayout>(R.id.settingsRoot)

        if (mainLayout != null) {
            addPanelToLayout(mainLayout)
        } else {
            val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(android.R.id.content)
            Toast.makeText(this, "Debug panel could not be added", Toast.LENGTH_SHORT).show()
        }
    }

    //debug panel for testing
    private fun addPanelToLayout(parentLayout: LinearLayout) {
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
                text = " DEBUG BILLING PANEL"
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
            addView(btnTestInterstitialShow)
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
}

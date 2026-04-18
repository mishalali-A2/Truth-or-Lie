package com.futurewatch.truthorlietv

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

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

        switchNetwork?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("network_sdk_enabled", isChecked).apply()
            val message = if (isChecked) "Analytics enabled" else "Analytics disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Remove Ads
        findViewById<View>(R.id.removeAds)?.setOnClickListener {
            Toast.makeText(this, "Remove Ads - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Restore Purchases
        findViewById<View>(R.id.restorePurchases)?.setOnClickListener {
            Toast.makeText(this, "Restore Purchases - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Rate App
        findViewById<View>(R.id.rateApp)?.setOnClickListener {
            Toast.makeText(this, "Thank you for rating!", Toast.LENGTH_SHORT).show()

        }

        // Terms & Privacy
        findViewById<View>(R.id.termsPrivacy)?.setOnClickListener {
            Toast.makeText(this, "Terms & Privacy - Coming soon!", Toast.LENGTH_SHORT).show()
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
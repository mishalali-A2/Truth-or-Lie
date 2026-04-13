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

        // Initialize views
        switchMusic = findViewById(R.id.switchMusic)
        backBtn = findViewById(R.id.btnBack)

        // Get all music chip buttons
        musicChips = listOf(
            findViewById(R.id.chipFunkyGroove) ?: findChipByText("Party Vibes"),
            findViewById(R.id.chipChillLounge) ?: findChipByText("Chill Lounge"),
            findViewById(R.id.chipRetroArcade) ?: findChipByText("Retro Arcade"),
            findViewById(R.id.chipPartyVibes) ?: findChipByText("Epic Adventure"),
            findViewById(R.id.chipEpicAdventure) ?: findChipByText("Funky Groove")
        ).filterNotNull()

        // Setup music switch
        switchMusic.isChecked = MusicManager.isEnabled()
        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            MusicManager.setEnabled(isChecked)
            val message = if (isChecked) "Music enabled" else "Music disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Setup music genre selection
        setupMusicGenreSelection()

        // Setup other settings (optional)
        setupOtherSettings()

        // Back button with focus animation
        setupFocusAnimation()

        backBtn.setOnClickListener {
            finish()
        }

        // Default focus
        backBtn.requestFocus()
    }

    private fun setupMusicGenreSelection() {
        val currentGenre = MusicManager.getCurrentGenre()

        // Find all music genre buttons by their text
        val musicContainer = findViewById<View>(R.id.musicContainer) ?: return

        // Get all buttons within the music card
        val allButtons = getAllButtonsInContainer(musicContainer)

        allButtons.forEach { button ->
            if (button is Button) {
                val buttonText = button.text.toString()

                // Check if this is a music genre button
                if (isMusicGenre(buttonText)) {
                    // Highlight current selection
                    if (buttonText == currentGenre) {
                        button.setBackgroundResource(R.drawable.chip_selected)
                    } else {
                        button.setBackgroundResource(R.drawable.chip_bg)
                    }

                    // Set click listener
                    button.setOnClickListener {
                        MusicManager.changeGenre(buttonText)
                        updateGenreSelection(buttonText)
                        Toast.makeText(this, "Music changed to: $buttonText", Toast.LENGTH_SHORT).show()
                    }

                    // Add focus animation for TV
                    button.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
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
        // Find button by text content
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
        // Timer selection
        setupTimerSelection()

        // Network SDK switch
        val switchNetwork = findViewById<SwitchCompat>(R.id.switchNetwork)
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        switchNetwork?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("network_sdk_enabled", isChecked).apply()
            val message = if (isChecked) "Analytics enabled" else "Analytics disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Remove Ads (placeholder)
        findViewById<View>(R.id.removeAds)?.setOnClickListener {
            Toast.makeText(this, "Remove Ads - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Restore Purchases (placeholder)
        findViewById<View>(R.id.restorePurchases)?.setOnClickListener {
            Toast.makeText(this, "Restore Purchases - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Rate App
        findViewById<View>(R.id.rateApp)?.setOnClickListener {
            Toast.makeText(this, "Thank you for rating!", Toast.LENGTH_SHORT).show()
            // TODO: Open Google Play rating dialog
        }

        // Terms & Privacy
        findViewById<View>(R.id.termsPrivacy)?.setOnClickListener {
            Toast.makeText(this, "Terms & Privacy - Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTimerSelection() {
        val timerContainer = findViewById<View>(R.id.timerContainer) ?: return
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTimer = prefs.getInt("timer_seconds", 20)

        val allButtons = getAllButtonsInContainer(timerContainer)

        allButtons.forEach { button ->
            if (button is Button) {
                val seconds = button.text.toString().toIntOrNull()
                if (seconds != null) {
                    // Highlight current selection
                    if (seconds == currentTimer) {
                        button.setBackgroundResource(R.drawable.chip_selected)
                    } else {
                        button.setBackgroundResource(R.drawable.chip_bg)
                    }

                    button.setOnClickListener {
                        prefs.edit().putInt("timer_seconds", seconds).apply()
                        updateTimerSelection(seconds)
                        Toast.makeText(this, "Timer set to $seconds seconds", Toast.LENGTH_SHORT).show()
                    }

                    // Focus animation
                    button.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                        } else {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        }
                    }
                }
            }
        }
    }

    private fun updateTimerSelection(selectedSeconds: Int) {
        val timerContainer = findViewById<View>(R.id.timerContainer) ?: return
        val allButtons = getAllButtonsInContainer(timerContainer)

        allButtons.forEach { button ->
            if (button is Button) {
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

    override fun onDestroy() {
        super.onDestroy()
    }
}
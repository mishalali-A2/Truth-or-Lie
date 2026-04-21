package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
class CategoriesActivity : AppCompatActivity() {

    private lateinit var unlockOverlay: View
    private lateinit var unlockTitle: TextView
    private lateinit var unlockDescription: TextView
    private lateinit var btnBuyCategory: Button
    private lateinit var btnWatchAd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

      //for testing
      //  resetAllPurchasesForTesting()
        MusicManager.resumeMusic()
        //        caused flickering -> parent in XML steals the focus
//        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
//            if (hasFocus) {
//                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scaleup))
//                v.elevation = 20f
//            } else {
//                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scaledown))
//                v.elevation = 0f
//            }
//        }


        // Initialize Overlay Views
        unlockOverlay = findViewById(R.id.unlockOverlay)
        unlockTitle = findViewById(R.id.unlockTitle)
        unlockDescription = findViewById(R.id.unlockDescription)
        btnBuyCategory = findViewById(R.id.btnBuyCategory)
        btnWatchAd = findViewById(R.id.btnWatchAd)

        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            v.clearAnimation()
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

        // Setup Overlay Button Animations
        btnBuyCategory.onFocusChangeListener = focusListener
        btnWatchAd.onFocusChangeListener = focusListener

        // Card -> Category
        val categoryMap = mapOf(
            R.id.card_general to "general_knowledge",
            R.id.card_science to "science",
            R.id.card_animals to "animals",
            R.id.card_history to "history",
            R.id.card_space to "space",
            R.id.card_technology to "technology",
            R.id.card_human_body to "human_body",
            R.id.card_crazy_facts to "crazy_facts",
            R.id.card_mixed_facts to "mixed_facts"
        )

        categoryMap.forEach { (id, categoryName) ->
            val view = findViewById<View>(id)

            view?.apply {
                isFocusable = true
                isClickable = true
                onFocusChangeListener = focusListener

                setOnClickListener {
                    if (CategoryManager.isUnlocked(categoryName)) {
                        GameSession.category = categoryName
                        val intent = Intent(this@CategoriesActivity, RoundsActivity::class.java)
                        startActivity(intent)
                    } else {
                        showUnlockOverlay(categoryName)
                    }
                }
            }
        }

        updateCategoryUI()
        
        // Handle back press to close overlay
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (unlockOverlay.visibility == View.VISIBLE) {
                    hideUnlockOverlay()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Default focus
        findViewById<View>(R.id.card_general)?.requestFocus()
    }

    private fun updateCategoryUI() {
        val prefs = TruthOrLieApplication.prefs
        val allCategoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)

        val lockedCategoryIds = listOf(
            R.id.card_history, R.id.card_space, R.id.card_technology,
            R.id.card_human_body, R.id.card_crazy_facts, R.id.card_mixed_facts
        )

        lockedCategoryIds.forEach { id ->
            val frameLayout = findViewById<View>(id) ?: return@forEach

            val categoryName = when (id) {
                R.id.card_history -> "history"
                R.id.card_space -> "space"
                R.id.card_technology -> "technology"
                R.id.card_human_body -> "human_body"
                R.id.card_crazy_facts -> "crazy_facts"
                R.id.card_mixed_facts -> "mixed_facts"
                else -> return@forEach
            }

            val isUnlocked = allCategoriesUnlocked || CategoryManager.isUnlocked(categoryName)

            if (isUnlocked) {
                if (frameLayout is android.view.ViewGroup && frameLayout.childCount >= 3) {
                    frameLayout.getChildAt(1).visibility = View.GONE
                    frameLayout.getChildAt(2).visibility = View.GONE

                    val contentLayout = frameLayout.getChildAt(0)
                    if (contentLayout is android.widget.LinearLayout) {
                        contentLayout.alpha = 1.0f
                        contentLayout.setBackgroundResource(R.drawable.card_bg)

                        val textView = contentLayout.getChildAt(1)
                        if (textView is android.widget.TextView) {
                            textView.alpha = 1.0f
                        }
                    }
                }
            }
        }
    }

    private fun showUnlockOverlay(category: String) {
        val friendlyName = category.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        
        unlockTitle.text = "Unlock $friendlyName"
        unlockDescription.text = "Get access to all $friendlyName statements and challenge your friends!"
        
        unlockOverlay.visibility = View.VISIBLE
        unlockOverlay.alpha = 0f
        unlockOverlay.animate().alpha(1f).setDuration(200).start()
        
        btnBuyCategory.setOnClickListener {
            val intent = Intent(this, PurchaseActivity::class.java)
            intent.putExtra("purpose", "unlock_categories")
            startActivity(intent)
        }
        
        btnWatchAd.setOnClickListener {
            if (!AdManager.isInitialized()) {
                Toast.makeText(this, "Ads loading, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Loading ad...", Toast.LENGTH_SHORT).show()
            AdManager.showRewardedAd(
                activity = this,
                onRewardEarned = {
                    runOnUiThread {
                        CategoryManager.unlockTemporarily(category)
                        Toast.makeText(this, "Category Unlocked! ✓", Toast.LENGTH_SHORT).show()
                        updateCategoryUI()
                        hideUnlockOverlay()
                    }
                },
                onFailed = {
                    runOnUiThread {
                        Toast.makeText(this, "Ad not available. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        
        btnBuyCategory.requestFocus()
    }

    private fun resetAllPurchasesForTesting() {
        val prefs = TruthOrLieApplication.prefs
        val hasBeenReset = prefs.getBoolean("debug_has_been_reset", false)

        if (!hasBeenReset) {
            // Clear all preferences
            prefs.edit().clear().apply()
            getSharedPreferences("app_settings", MODE_PRIVATE).edit().clear().apply()
            CategoryManager.resetSession()

            // Reset billing
            try {
                TruthOrLieApplication.billingRepository.clearPurchaseCache()
                TruthOrLieApplication.billingRepository.resetBillingState()
            } catch (e: Exception) {
                Log.e("ResetDebug", "Error resetting billing", e)
            }

            // Set default values
            prefs.edit()
                .putBoolean("music_enabled", true)
                .putInt("timer_seconds", 20)
                .putBoolean("network_sdk_enabled", false)
                .putBoolean("debug_has_been_reset", true)
                .apply()

            Log.d("ResetDebug", "========== RESET COMPLETE (WILL NOT RUN AGAIN) ==========")

            Toast.makeText(this, "✅ Purchases reset for testing", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("ResetDebug", "Skipping reset - already performed once")
        }
    }
    private fun hideUnlockOverlay() {
        unlockOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            unlockOverlay.visibility = View.GONE
            findViewById<View>(R.id.card_general)?.requestFocus()
        }.start()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
        
        val prefs = TruthOrLieApplication.prefs
        val allCategoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)

        if (allCategoriesUnlocked) {
            val allCategories = listOf(
                "history", "space", "technology", "human_body", "crazy_facts", "mixed_facts"
            )
            allCategories.forEach { category ->
                CategoryManager.unlockTemporarily(category)
            }
            updateCategoryUI()
        } else {
            updateCategoryUI()
        }
    }
}

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
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.InputTracker
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class CategoriesActivity : AppCompatActivity() {

    // Idle-gap aggregator for the 15-card grid — avoids flooding analytics with a step-by-step
    // event for every transient D-pad focus move; emits one focus_settled ~500ms after the user
    // stops moving (see InputTracker.FocusIdleAggregator).
    private val gridFocusAggregator = InputTracker.FocusIdleAggregator(AnalyticsScreens.CATEGORIES)

    // Tracks which categories were seen unlocked on the previous UI refresh, purely so a
    // silent unlocked -> locked transition (the 24h temporary unlock expiring between visits,
    // detected via CategoryManager.isUnlocked()'s own read-time expiry check) can be surfaced as
    // CATEGORY_UNLOCK_EXPIRED without modifying CategoryManager itself.
    private var previouslyUnlockedCategories: Set<String>? = null

    private lateinit var unlockOverlay: View
    private lateinit var unlockTitle: TextView
    private lateinit var unlockDescription: TextView
    private lateinit var btnBuyCategory: Button
    private lateinit var btnWatchAd: Button

    // Map category names to their product IDs
    private val categoryProductMap = mapOf(
        "history" to "buy.history",
        "space" to "buy.space",
        "technology" to "buy.technology",
        "human_body" to "buy.humanbodycategory",
        "crazy_facts" to "buy.crazyfacts",
        "money_luxury" to "buy.money",
        "movie" to "buy.movies",
        "random_chaos" to "buy.partymode",  // Party Mode
        "relations_social" to "buy.relationships",
        "survival" to "buy.survival",
        "travel" to "buy.adventure",
        "family_mode" to "buy.familymode"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
//for testing purposes
        // resetAllPurchasesOnLaunch()
        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.CATEGORIES, previousScreen = AnalyticsScreens.MAIN)


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
        btnBuyCategory.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            focusListener.onFocusChange(v, hasFocus)
            InputTracker.logFocusChanged(AnalyticsScreens.CATEGORIES, "categories_buy_category", hasFocus)
        }
        btnWatchAd.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            focusListener.onFocusChange(v, hasFocus)
            InputTracker.logFocusChanged(AnalyticsScreens.CATEGORIES, "categories_watch_ad", hasFocus)
        }

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
            R.id.card_money_luxury to "money_luxury",
            R.id.card_movie to "movie",
            R.id.card_random_chaos to "random_chaos",
            R.id.card_relations_social to "relations_social",
            R.id.card_survival to "survival",
            R.id.card_travel to "travel",
            R.id.card_family_mode to "family_mode"
        )

        categoryMap.forEach { (id, categoryName) ->
            val view = findViewById<View>(id)

            view?.apply {
                isFocusable = true
                isClickable = true
                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    focusListener.onFocusChange(v, hasFocus)
                    // High-frequency grid: aggregate via idle-gap debounce rather than logging
                    // every transient step (see InputTracker.FocusIdleAggregator, 500ms window).
                    if (hasFocus) gridFocusAggregator.onFocusChanged("categories_card_$categoryName")
                }

                setOnClickListener {
                    if (CategoryManager.isUnlocked(categoryName)) {
                        AnalyticsService.logEvent(
                            AnalyticsEvents.CATEGORY_SELECTED,
                            mapOf(AnalyticsParams.CATEGORY_ID to categoryName)
                        )
                        GameSession.category = categoryName
                        val intent = Intent(this@CategoriesActivity, RoundsActivity::class.java)
                        startActivity(intent)
                    } else {
                        AnalyticsService.logEvent(
                            AnalyticsEvents.CATEGORY_LOCKED_VIEWED,
                            mapOf(AnalyticsParams.CATEGORY_ID to categoryName)
                        )
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
                    AnalyticsService.logEvent(
                        AnalyticsEvents.CONTROL_CLICK,
                        mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.CATEGORIES, AnalyticsParams.CONTROL_ID to "categories_overlay_back")
                    )
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
            R.id.card_human_body, R.id.card_crazy_facts,
            R.id.card_money_luxury, R.id.card_movie , R.id.card_random_chaos,
            R.id.card_relations_social , R.id.card_survival, R.id.card_travel,
            R.id.card_family_mode
        )

        val currentlyUnlockedCategories = mutableSetOf<String>()

        lockedCategoryIds.forEach { id ->
            val frameLayout = findViewById<View>(id) ?: return@forEach

            val categoryName = when (id) {
                R.id.card_history -> "history"
                R.id.card_space -> "space"
                R.id.card_technology -> "technology"
                R.id.card_human_body -> "human_body"
                R.id.card_crazy_facts -> "crazy_facts"
                R.id.card_money_luxury -> "money_luxury"
                R.id.card_movie -> "movie"
                R.id.card_random_chaos -> "random_chaos"
                R.id.card_relations_social -> "relations_social"
                R.id.card_survival -> "survival"
                R.id.card_travel -> "travel"
                R.id.card_family_mode -> "family_mode"
                else -> return@forEach
            }

            val isUnlocked = allCategoriesUnlocked || CategoryManager.isUnlocked(categoryName)
            if (isUnlocked) currentlyUnlockedCategories.add(categoryName)

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

        // A category that was unlocked on a prior refresh but is no longer unlocked now (without
        // all_categories_unlocked ever being toggled off) means its 24h temporary unlock expired.
        previouslyUnlockedCategories?.let { previous ->
            if (!allCategoriesUnlocked) {
                (previous - currentlyUnlockedCategories).forEach { expiredCategory ->
                    AnalyticsService.logEvent(
                        AnalyticsEvents.CATEGORY_UNLOCK_EXPIRED,
                        mapOf(AnalyticsParams.CATEGORY_ID to expiredCategory)
                    )
                }
            }
        }
        previouslyUnlockedCategories = currentlyUnlockedCategories
    }

    private fun showUnlockOverlay(category: String) {
        val friendlyName = category.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        
        unlockTitle.text = "Unlock $friendlyName"
        unlockDescription.text = "Get access to all $friendlyName statements and challenge your friends!"
        
        unlockOverlay.visibility = View.VISIBLE
        unlockOverlay.alpha = 0f
        unlockOverlay.animate().alpha(1f).setDuration(200).start()
        
        btnBuyCategory.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.CATEGORIES, AnalyticsParams.CONTROL_ID to "categories_buy_category")
            )
            purchaseSingleCategory(category)
        }

        btnWatchAd.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.CATEGORIES, AnalyticsParams.CONTROL_ID to "categories_watch_ad")
            )
            if (!AdManager.isInitialized()) {
                com.futurewatch.truthorlietv.analytics.AdAnalyticsTracker.logShowSkippedNotInitialized(
                    com.futurewatch.truthorlietv.analytics.AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED,
                    com.futurewatch.truthorlietv.analytics.AdAnalyticsTracker.FORMAT_REWARDED
                )
                Toast.makeText(this, "Ads loading, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Loading ad...", Toast.LENGTH_SHORT).show()
            AdManager.showRewardedAd(
                activity = this,
                onRewardEarned = {
                    runOnUiThread {
                        AnalyticsService.logEvent(
                            AnalyticsEvents.CATEGORY_UNLOCK_AD_WATCHED,
                            mapOf(AnalyticsParams.CATEGORY_ID to category)
                        )
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

    private fun resetAllPurchasesOnLaunch() {
        val prefs = TruthOrLieApplication.prefs
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
            .apply()

        Log.d("ResetDebug", "========== RESET COMPLETE (ALWAYS RUNS ON LAUNCH) ==========")
        Toast.makeText(this, "✅ Purchases reset, all categories locked", Toast.LENGTH_SHORT).show()
    }

    private fun hideUnlockOverlay() {
        unlockOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            unlockOverlay.visibility = View.GONE
            findViewById<View>(R.id.card_general)?.requestFocus()
        }.start()
    }

    override fun onPause() {
        super.onPause()
        gridFocusAggregator.cancel()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()

        val prefs = TruthOrLieApplication.prefs
        val allCategoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)

        if (allCategoriesUnlocked) {
            val allCategories = listOf(
                "history", "space", "technology", "human_body", "crazy_facts", "money_luxury", "movie",  "random_chaos", "relations_social", "survival", "travel", "family_mode"
            )
            allCategories.forEach { category ->
                CategoryManager.unlockTemporarily(category)
            }
            updateCategoryUI()
        } else {
            updateCategoryUI()
        }
    }

    private fun purchaseSingleCategory(category: String) {
        Log.d("CategoriesActivity", "Initiating purchase for category: $category")

        // Get the specific product ID for this category
        val productId = categoryProductMap[category]
        if (productId == null) {
            Log.e("CategoriesActivity", "No product ID mapping for category: $category")
            Toast.makeText(this, "Error: Category not found", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Loading payment...", Toast.LENGTH_SHORT).show()

        try {
            // Use category-specific product ID (e.g., buy.history, buy.technology)
            Log.d("CategoriesActivity", "Purchasing productId=$productId for category=$category")
            AnalyticsService.logEvent(
                AnalyticsEvents.PURCHASE_INITIATED,
                mapOf(AnalyticsParams.PRODUCT_ID to productId, AnalyticsParams.CATEGORY_ID to category)
            )
            TruthOrLieApplication.billingRepository.purchaseProduct(this, productId, category)
            // Hide overlay after purchase initiated
            hideUnlockOverlay()
        } catch (e: Exception) {
            Log.e("CategoriesActivity", "Error initiating purchase", e)
            AnalyticsService.logEvent(
                AnalyticsEvents.ERROR_BILLING,
                mapOf(AnalyticsParams.ERROR_CATEGORY to "purchase_flow_launch_failed")
            )
            Toast.makeText(this, "Purchase error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

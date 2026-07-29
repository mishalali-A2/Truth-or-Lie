package com.futurewatch.truthorlietv

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class PurchaseActivity : AppCompatActivity() {

    private lateinit var btnPurchase: Button
    private lateinit var btnCancel: Button
    private lateinit var txtRestore: TextView
    private lateinit var titlePremium: TextView
    private lateinit var description: TextView
    
    private var purchaseType = "all_categories"  // Default: unlock all categories
    private var selectedCategory: String? = null // For single category unlock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.purchase)

        ScreenTracker.attach(this, AnalyticsScreens.PURCHASE)

        btnPurchase = findViewById(R.id.btnPurchase)
        btnCancel = findViewById(R.id.btnCancel)
        txtRestore = findViewById(R.id.txtRestore)
        titlePremium = findViewById(R.id.titlePremium)
        description = findViewById(R.id.description)

        // Get purchase type and category from intent
        purchaseType = intent.getStringExtra("purpose") ?: "all_categories"
        selectedCategory = intent.getStringExtra("category")

        // Check if already purchased
        val prefs = TruthOrLieApplication.prefs
        val allCategoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)

        // Update UI based on purchase type
        when (purchaseType) {
            "unlock_single_category" -> {
                val friendlyName = selectedCategory?.replace("_", " ")?.split(" ")?.joinToString(" ") {
                    it.replaceFirstChar { char -> char.uppercase() }
                } ?: "Category"

                titlePremium.text = "Unlock $friendlyName"
                description.text = "Get 24-hour access to all $friendlyName statements!"

                val categoryProductId = BillingManager.PRODUCT_TO_CATEGORY.entries
                    .firstOrNull { it.value == selectedCategory }?.key ?: "premium.unlock_category"
                val livePrice = TruthOrLieApplication.billingRepository
                    .getProductPrice(categoryProductId) ?: "$2.99"
                btnPurchase.text = "Buy for $livePrice"
            }
            else -> {
                if (allCategoriesUnlocked) {
                    titlePremium.text = "ALREADY UNLOCKED!"
                    description.text = "You already have full access to all categories!"
                    btnPurchase.text = "Unlocked ✓"
                    btnPurchase.isEnabled = false
                    btnPurchase.alpha = 0.5f
                } else {
                    titlePremium.text = "All Categories"
                    description.text = "Unlock all categories and get permanent access!"
                    val livePrice = TruthOrLieApplication.billingRepository
                        .getProductPrice("premium.access") ?: "$5.99"
                    btnPurchase.text = "Unlock All Categories for $livePrice"
                }
            }
        }

        // Purchase button click
        btnPurchase.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.PURCHASE, AnalyticsParams.CONTROL_ID to "purchase_buy")
            )
            if (purchaseType == "unlock_single_category") {
                purchaseProduct("premium.unlock_category")
            } else {
                if (allCategoriesUnlocked) {
                    Toast.makeText(this, "Already unlocked!", Toast.LENGTH_SHORT).show()
                    finish()
                    return@setOnClickListener
                }
                purchaseProduct("premium.access")
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.PURCHASE, AnalyticsParams.CONTROL_ID to "purchase_cancel")
            )
            finish()
        }

        // Restore purchases
        txtRestore.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.PURCHASE, AnalyticsParams.CONTROL_ID to "purchase_restore")
            )
            restorePurchases()
        }

        // Handle back press - close overlay
        btnCancel.requestFocus()

        setupFocusAnimation()
    }

    private fun purchaseProduct(productId: String) {
        val prefs = TruthOrLieApplication.prefs

        // Check if already owned for all categories
        if (productId == "premium.access" && prefs.getBoolean("all_categories_unlocked", false)) {
            Toast.makeText(this, "Already unlocked!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PurchaseActivity", "Initiating purchase for: $productId (category: $selectedCategory)")
        try {
            AnalyticsService.logEvent(
                AnalyticsEvents.PURCHASE_INITIATED,
                mapOf(AnalyticsParams.PRODUCT_ID to productId)
            )
            TruthOrLieApplication.billingRepository.purchaseProduct(this, productId, selectedCategory)
        } catch (e: Exception) {
            Log.e("PurchaseActivity", "Error launching purchase flow", e)
            AnalyticsService.logEvent(
                AnalyticsEvents.ERROR_BILLING,
                mapOf(AnalyticsParams.ERROR_CATEGORY to "purchase_flow_launch_failed")
            )
            Toast.makeText(this, "Billing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restorePurchases() {
        Log.d("PurchaseActivity", "Restoring purchases...")
        AnalyticsService.logEvent(AnalyticsEvents.RESTORE_PURCHASES_REQUESTED)
        Toast.makeText(this, "Checking for previous purchases...", Toast.LENGTH_SHORT).show()
        TruthOrLieApplication.billingRepository.restorePurchases()

        // Check after restore
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val prefs = TruthOrLieApplication.prefs
            if (prefs.getBoolean("all_categories_unlocked", false)) {
                Toast.makeText(this, "Purchase restored! All categories unlocked.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "No previous purchases found.", Toast.LENGTH_SHORT).show()
            }
        }, 2000)
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

        btnPurchase.onFocusChangeListener = focusListener
        btnCancel.onFocusChangeListener = focusListener
    }

    override fun onResume() {
        super.onResume()
        // Update UI in case purchase completed while away
        val prefs = TruthOrLieApplication.prefs
        if (prefs.getBoolean("all_categories_unlocked", false)) {
            finish() // Close if now unlocked
        }
    }
}
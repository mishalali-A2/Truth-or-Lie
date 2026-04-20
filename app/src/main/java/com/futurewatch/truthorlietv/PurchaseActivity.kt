package com.futurewatch.truthorlietv

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class PurchaseActivity : AppCompatActivity() {

    private lateinit var btnRemoveAds: Button
    private lateinit var btnUnlockCategories: Button
    private lateinit var btnMonthlySubscription: Button
    private lateinit var btnYearlySubscription: Button
    private lateinit var btnRestore: Button
    private lateinit var btnBack: Button
    private lateinit var tvPremiumStatus: TextView

    private var purchasePurpose: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.purchase)

        // Get purpose from intent
        purchasePurpose = intent.getStringExtra("purpose")

        btnRemoveAds = findViewById(R.id.btnRemoveAds)
        btnUnlockCategories = findViewById(R.id.btnUnlockCategories)
        btnMonthlySubscription = findViewById(R.id.btnMonthlySubscription)
        btnYearlySubscription = findViewById(R.id.btnYearlySubscription)
        btnRestore = findViewById(R.id.btnRestore)
        btnBack = findViewById(R.id.btnBack)
        tvPremiumStatus = findViewById(R.id.tvPremiumStatus)

        // Show current premium status
        updatePremiumStatus()

        // Highlight the relevant purchase option if coming from a specific flow
        if (purchasePurpose == "unlock_categories") {
            highlightUnlockCategories()
        }

        // Setup click listeners
        btnRemoveAds.setOnClickListener {
            purchaseProduct("remove_ads")
        }

        btnUnlockCategories.setOnClickListener {
            purchaseProduct("unlock_all_categories")
        }

        btnMonthlySubscription.setOnClickListener {
            purchaseProduct("premium_monthly")
        }

        btnYearlySubscription.setOnClickListener {
            purchaseProduct("premium_yearly")
        }

        btnRestore.setOnClickListener {
            restorePurchases()
        }

        btnBack.setOnClickListener {
            finish()
        }

        setupFocusAnimation()
    }

    private fun highlightUnlockCategories() {
        // Highlight the unlock categories button
        btnUnlockCategories.setBackgroundColor(android.graphics.Color.parseColor("#FFA500"))
        btnUnlockCategories.requestFocus()
        Toast.makeText(this, "Unlock all categories to access locked content!", Toast.LENGTH_LONG).show()
    }

    private fun purchaseProduct(productId: String) {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        // Check if already owned in local prefs
        when (productId) {
            "remove_ads" -> {
                if (prefs.getBoolean("ads_removed", false)) {
                    Toast.makeText(this, "Ads already removed!", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            "unlock_all_categories" -> {
                if (prefs.getBoolean("all_categories_unlocked", false)) {
                    Toast.makeText(this, "All categories already unlocked!", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        // Use the real BillingRepository instead of simulation
        Log.d("PurchaseActivity", "Initiating real purchase for: $productId")
        try {
            TruthOrLieApplication.billingRepository.purchaseProduct(this, productId)
        } catch (e: Exception) {
            Log.e("PurchaseActivity", "Error launching purchase flow", e)
            Toast.makeText(this, "Billing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restorePurchases() {
        Log.d("PurchaseActivity", "Restoring purchases...")
        Toast.makeText(this, "Checking for previous purchases...", Toast.LENGTH_SHORT).show()
        TruthOrLieApplication.billingRepository.restorePurchases()
        
        // The update will happen via the global listener in Application class
        // which updates prefs. We'll refresh our UI shortly.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updatePremiumStatus()
        }, 2000)
    }

    private fun updatePremiumStatus() {
        val prefs = TruthOrLieApplication.prefs
        val adsRemoved = prefs.getBoolean("ads_removed", false)
        val categoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)
        val premiumAccess = prefs.getBoolean("premium_access", false)

        val status = buildString {
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("     YOUR PURCHASE STATUS\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            if (adsRemoved) append("✅ Ads Removed\n")
            if (categoriesUnlocked) append("✅ All Categories Unlocked\n")
            if (premiumAccess) append("✅ Premium Active\n")
            if (!adsRemoved && !categoriesUnlocked && !premiumAccess) {
                append("❌ No active purchases\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }

        tvPremiumStatus.text = status
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

        btnRemoveAds.onFocusChangeListener = focusListener
        btnUnlockCategories.onFocusChangeListener = focusListener
        btnMonthlySubscription.onFocusChangeListener = focusListener
        btnYearlySubscription.onFocusChangeListener = focusListener
        btnRestore.onFocusChangeListener = focusListener
        btnBack.onFocusChangeListener = focusListener
    }

    override fun onResume() {
        super.onResume()
        updatePremiumStatus()
    }
}

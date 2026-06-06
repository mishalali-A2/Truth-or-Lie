package com.futurewatch.truthorlietv

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


class BillingManager(
    private val context: Context,
    private val listener: BillingListener

) : PurchasesUpdatedListener, BillingClientStateListener {
    private val isFakeBilling get() = false
    interface BillingListener {
        fun onBillingSetupFinished()
        fun onBillingDisconnected()
        fun onProductsUpdated(products: List<ProductDetails>)
        fun onPurchaseSuccess(productId: String)
        fun onPurchaseError(responseCode: Int, message: String?)
        fun onPurchaseCanceled()
        fun onRestoreCompleted(hasPremium: Boolean)
    }

    companion object {
        private const val TAG = "BillingManager"
        private const val PREFS_NAME = "billing_prefs"
        private const val KEY_PREMIUM = "premium_access"

        // product ids: In-app products for unlocking categories and removing ads
        val INAPP_PRODUCT_IDS = listOf(
            "premium.access",              // $5.99 - Removes ads and unlocks all categories
            "premium.unlock_category",     // $2.99 - Unlocks a single category for 24 hours
            "buy.history",                 // History category
            "buy.space",                   // Space category
            "buy.technology",              // Technology category
            "buy.humanbodycategory",       // Human Body category
            "buy.crazyfacts",              // Crazy Facts category
            "buy.partymode",               // Party Mode
            "buy.relationships",           // Relationships category
            "buy.adventure",               // Adventure category
            "buy.money",                   // Money & Luxury category
            "buy.movies",                  // Movies category
            "buy.survival",                // Survival category
            "buy.familymode"               // Family Mode
        )

        val SUBS_PRODUCT_IDS = listOf(
            "premium_monthly",      // Monthly subscription
            "premium_yearly"        // Yearly subscription
        )

        // All product IDs combined
        val ALL_PRODUCT_IDS = INAPP_PRODUCT_IDS + SUBS_PRODUCT_IDS

        // Premium product IDs (anything that gives premium access)
        val PREMIUM_PRODUCT_IDS = setOf(
            "premium.access",
        )

        // Maps each category product ID directly to its category key — no SharedPrefs needed
        val PRODUCT_TO_CATEGORY = mapOf(
            "buy.history"           to "history",
            "buy.space"             to "space",
            "buy.technology"        to "technology",
            "buy.humanbodycategory" to "human_body",
            "buy.crazyfacts"        to "crazy_facts",
            "buy.partymode"         to "random_chaos",
            "buy.relationships"     to "relations_social",
            "buy.adventure"         to "travel",
            "buy.money"             to "money_luxury",
            "buy.movies"            to "movie",
            "buy.survival"          to "survival",
            "buy.familymode"        to "family_mode",
            "premium.unlock_category" to null
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private var isServiceConnected = false
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    // Used to safely merge results from concurrent INAPP + SUBS purchase queries
    private val pendingQueryCount = AtomicInteger(0)
    private val resolvedPremiumAccess = AtomicBoolean(false)
    fun initialize() {
        Log.d(TAG, "Initializing BillingClient")
        if (billingClient.isReady) {
            Log.d(TAG, "BillingClient already ready, skipping startConnection")
            isServiceConnected = true
            listener.onBillingSetupFinished()
            queryProductDetails()
            queryPurchases()
            return
        }

        Log.d(TAG, "Starting BillingClient connection...")
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "onBillingSetupFinished called with responseCode=${billingResult.responseCode}")

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isServiceConnected = true
            Log.d(TAG, "✓ Billing setup SUCCESSFUL - service connected")
            listener.onBillingSetupFinished()

            // Query products and purchases after successful connection
            queryProductDetails()
            queryPurchases()
        } else {
            Log.e(
                TAG,
                "✗ Billing setup FAILED with responseCode=${billingResult.responseCode}, " +
                        "debugMessage=${billingResult.debugMessage}"
            )
            isServiceConnected = false
            listener.onPurchaseError(
                billingResult.responseCode,
                "Billing setup failed: ${billingResult.debugMessage}"
            )
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "onBillingServiceDisconnected - service disconnected, attempting reconnect")
        isServiceConnected = false
        listener.onBillingDisconnected()

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Attempting to reconnect to billing service...")
            if (!billingClient.isReady) {
                billingClient.startConnection(this)
            }
        }, 2000)
    }

    // PRODUCT QUERIES

    fun queryProductDetails() {
        if (isFakeBilling) {
            Log.d(TAG, "⚡ Fake product loading")

            listener.onProductsUpdated(emptyList())
            return
        }
        if (!isServiceConnected) {
            Log.w(TAG, "queryProductDetails called but service not connected, skipping")
            return
        }

        Log.d(TAG, "Querying product details for INAPP and SUBS...")

        // QUERY 1: INAPP PRODUCTS (remove_ads, unlock_all_categories)
        if (INAPP_PRODUCT_IDS.isNotEmpty()) {
            queryInAppProducts()
        }

        // QUERY 2: SUBS PRODUCTS (premium_monthly, premium_yearly)
        if (SUBS_PRODUCT_IDS.isNotEmpty()) {
            querySubsProducts()
        }
    }

    private fun queryInAppProducts() {
        Log.d(TAG, "Querying INAPP products: $INAPP_PRODUCT_IDS")

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                INAPP_PRODUCT_IDS.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(
                    TAG,
                    "✗ Failed to query INAPP products (code=${billingResult.responseCode}): ${billingResult.debugMessage}"
                )
                return@queryProductDetailsAsync
            }

            productDetailsList.forEach { details ->
                productDetailsMap[details.productId] = details
                val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
                Log.d(TAG, "✓ INAPP Product loaded: ${details.productId} - ${details.title} - $price")
            }
        }
    }

    private fun querySubsProducts() {
        Log.d(TAG, "Querying SUBS products: $SUBS_PRODUCT_IDS")

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                SUBS_PRODUCT_IDS.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(
                    TAG,
                    "✗ Failed to query SUBS products (code=${billingResult.responseCode}): ${billingResult.debugMessage}"
                )
                return@queryProductDetailsAsync
            }

            productDetailsList.forEach { details ->
                productDetailsMap[details.productId] = details
                val price = details.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice ?: "N/A"
                Log.d(TAG, "✓ SUBS Product loaded: ${details.productId} - ${details.title} - $price")
            }

            listener.onProductsUpdated(productDetailsList)
        }
    }

    // PURCHASE QUERIES (RESTORE)

    fun queryPurchases() {
        if (isFakeBilling) {
            Log.d(TAG, "⚡ Fake restore")
            listener.onRestoreCompleted(getHasPremiumAccess())
            return
        }
        if (!isServiceConnected) {
            Log.w(TAG, "queryPurchases called but service not connected, skipping")
            return
        }

        Log.d(TAG, "Querying existing purchases...")

        // Reset merge state before firing both queries
        pendingQueryCount.set(2)
        resolvedPremiumAccess.set(false)

        queryInAppPurchases()
        querySubsPurchases()
    }

    private fun queryInAppPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Query INAPP purchases failed (code=${billingResult.responseCode}): ${billingResult.debugMessage}")
                onQueryComplete(false, purchases)
                return@queryPurchasesAsync
            }
            Log.d(TAG, "INAPP purchases query returned ${purchases.size} purchases")
            onQueryComplete(processRawPurchases(purchases), purchases)
        }
    }

    private fun querySubsPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Query SUBS purchases failed (code=${billingResult.responseCode}): ${billingResult.debugMessage}")
                onQueryComplete(false, purchases)
                return@queryPurchasesAsync
            }
            Log.d(TAG, "SUBS purchases query returned ${purchases.size} purchases")
            onQueryComplete(processRawPurchases(purchases), purchases)
        }
    }

    // Processes one batch of purchases and returns whether any grant premium access.
    // Does NOT call the listener — that only happens once both queries are done.
    private fun processRawPurchases(purchases: List<Purchase>): Boolean {
        var hasPremium = false
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue

            purchase.products.forEach { productId ->
                when {
                    productId == "premium.access" -> {
                        prefs.edit()
                            .putBoolean("ads_removed", true)
                            .putBoolean("all_categories_unlocked", true)
                            .apply()
                        hasPremium = true
                    }
                    productId in setOf("premium_monthly", "premium_yearly") -> {
                        prefs.edit()
                            .putBoolean("ads_removed", true)
                            .putBoolean("all_categories_unlocked", true)
                            .putBoolean("premium_access", true)
                            .apply()
                        hasPremium = true
                    }
                    productId in INAPP_PRODUCT_IDS && productId != "premium.access" -> {
                        // Resolve category directly from the product ID — no SharedPrefs dependency
                        val category = PRODUCT_TO_CATEGORY[productId]
                        if (category != null) {
                            prefs.edit()
                                .putBoolean("category_${category}_unlocked_24h", true)
                                .putLong("category_${category}_unlock_time", System.currentTimeMillis())
                                .apply()
                            Log.d(TAG, "Category unlocked for 24h: $category (productId: $productId)")
                        }
                    }
                }
                Log.d(TAG, "✓ Purchase processed: $productId")
            }

            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
        }

        return hasPremium
    }

    // Called once per query (INAPP and SUBS). Only fires the listener after both complete.
    private fun onQueryComplete(hasPremium: Boolean, purchases: List<Purchase>) {
        if (hasPremium) resolvedPremiumAccess.set(true)

        if (pendingQueryCount.decrementAndGet() == 0) {
            val finalResult = resolvedPremiumAccess.get()
            setHasPremiumAccess(finalResult)
            listener.onRestoreCompleted(finalResult)
        }
    }

    // Called for immediate new purchases (not restore queries) — single batch, no counting needed.
    private fun handlePurchases(purchases: List<Purchase>) {
        val hasPremium = processRawPurchases(purchases)
        if (hasPremium) setHasPremiumAccess(true)
        listener.onRestoreCompleted(hasPremium || getHasPremiumAccess())
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✓ Purchase acknowledged: ${purchase.purchaseToken.take(8)}...")
            } else {
                Log.e(
                    TAG,
                    "✗ Failed to acknowledge purchase: ${billingResult.debugMessage}"
                )
            }
        }
    }
    fun launchPurchaseFlow(activity: Activity, productId: String, category: String? = null) {
        Log.d(TAG, "launchPurchaseFlow called for productId=$productId (category=$category)")

        if (isFakeBilling) {
            Log.d(TAG, "⚡ Using FAKE billing for $productId")

            Handler(Looper.getMainLooper()).postDelayed({
                handleFakePurchase(productId)
            }, 1000)

            return
        }

        //  REAL BILLING
        if (!isServiceConnected) {
            Log.e(TAG, "✗ Launch blocked: billing service disconnected")
            listener.onPurchaseError(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                "Billing service not connected"
            )
            return
        }

        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            Log.e(TAG, "✗ Launch blocked: product unavailable for id=$productId")
            Log.d(TAG, "Available products: ${productDetailsMap.keys}")
            listener.onPurchaseError(
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                "Product not found: $productId"
            )
            return
        }

        val detailsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // Prefer the offer with a free trial or intro price; fall back to the base plan
        productDetails.subscriptionOfferDetails
            ?.let { offers ->
                val bestOffer = offers.firstOrNull { offer ->
                    offer.pricingPhases.pricingPhaseList.any { phase ->
                        phase.priceAmountMicros == 0L || phase.billingCycleCount > 0
                    }
                } ?: offers.firstOrNull()
                bestOffer?.let { detailsBuilder.setOfferToken(it.offerToken) }
            }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(detailsBuilder.build()))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun getProductPrice(productId: String): String? {
        val details = productDetailsMap[productId] ?: return null
        return details.oneTimePurchaseOfferDetails?.formattedPrice
            ?: details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                ?.formattedPrice
    }

    fun clearPurchaseCache() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("ads_removed")
            .remove("all_categories_unlocked")
            .remove("premium_access")
            .apply()
        Log.d(TAG, "Purchase cache cleared")
    }

    fun resetBillingState() {
        productDetailsMap.clear()
        isServiceConnected = false
        Log.d(TAG, "Billing state reset")
    }
    private fun handleFakePurchase(productId: String) {
        Log.d(TAG, "✅ FAKE purchase success: $productId")

        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        when (productId) {
            "premium.access" -> {
                prefs.edit()
                    .putBoolean("ads_removed", true)
                    .putBoolean("all_categories_unlocked", true)
                    .apply()
                setHasPremiumAccess(true)
            }
            in INAPP_PRODUCT_IDS -> {
                // Resolve category directly from product ID — same as real purchase flow
                val category = PRODUCT_TO_CATEGORY[productId]
                if (category != null) {
                    prefs.edit()
                        .putBoolean("category_${category}_unlocked_24h", true)
                        .putLong("category_${category}_unlock_time", System.currentTimeMillis())
                        .apply()
                    Log.d(TAG, "Category unlocked for 24h: $category (productId: $productId)")
                }
            }
            "premium_monthly", "premium_yearly" -> {
                prefs.edit()
                    .putBoolean("ads_removed", true)
                    .putBoolean("all_categories_unlocked", true)
                    .putBoolean("premium_access", true)
                    .apply()
                setHasPremiumAccess(true)
            }
        }

        listener.onPurchaseSuccess(productId)
        listener.onRestoreCompleted(true)
    }
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        Log.d(TAG, "onPurchasesUpdated called with responseCode=${billingResult.responseCode}")

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val localPurchases = purchases ?: emptyList()
                Log.d(TAG, "✓ Purchase update success; count=${localPurchases.size}")
                handlePurchases(localPurchases)

                localPurchases.forEach { purchase ->
                    purchase.products.forEach { productId ->
                        Log.d(TAG, "✓ Purchase success for productId=$productId")
                        listener.onPurchaseSuccess(productId)
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned, restoring purchases")
                queryPurchases()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase canceled by user")
                listener.onPurchaseCanceled()
            }
            else -> {
                Log.e(
                    TAG,
                    "✗ Purchase failed: code=${billingResult.responseCode}, " +
                            "message=${billingResult.debugMessage}"
                )
                listener.onPurchaseError(billingResult.responseCode, billingResult.debugMessage)
            }
        }
    }
    fun restorePurchases() {
        Log.d(TAG, "restorePurchases called")
        queryPurchases()
    }

    fun getHasPremiumAccess(): Boolean {
        return prefs.getBoolean(KEY_PREMIUM, false)
    }

    private fun setHasPremiumAccess(hasAccess: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, hasAccess).apply()
        Log.d(TAG, "Premium access set to: $hasAccess")
    }

    fun destroy() {
        Log.d(TAG, "Destroying BillingManager")
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
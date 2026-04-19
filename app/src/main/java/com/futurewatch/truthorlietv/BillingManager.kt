package com.futurewatch.truthorlietv

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val listener: BillingListener
) : PurchasesUpdatedListener, BillingClientStateListener {

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

        // Define your product IDs (create these in Google Play Console)
        val PRODUCT_IDS = listOf(
            "remove_ads",           // One-time purchase to remove ads
            "premium_monthly",      // Monthly subscription
            "premium_yearly",       // Yearly subscription
            "unlock_all_categories" // One-time unlock all categories
        )

        // Premium product IDs (anything that gives premium access)
        val PREMIUM_PRODUCT_IDS = setOf(
            "remove_ads",
            "premium_monthly",
            "premium_yearly",
            "unlock_all_categories"
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
    private val purchaseAttemptIds = mutableMapOf<String, String>()

    fun initialize() {
        Log.d(TAG, "Initializing BillingClient")
        if (billingClient.isReady) {
            isServiceConnected = true
            listener.onBillingSetupFinished()
            queryProductDetails()
            queryPurchases()
            return
        }
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "Billing setup finished: ${billingResult.responseCode}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isServiceConnected = true
            listener.onBillingSetupFinished()
            queryProductDetails()
            queryPurchases()
        } else {
            Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
            // Don't crash, just log and continue
            isServiceConnected = false
            // Don't call listener.onBillingDisconnected() as it might cause issues
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected")
        isServiceConnected = false
        listener.onBillingDisconnected()
        // Attempt to reconnect
        billingClient.startConnection(this)
    }

    fun queryProductDetails() {
        if (!isServiceConnected) return

        Log.d(TAG, "Querying product details for: $PRODUCT_IDS")

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PRODUCT_IDS.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                } + listOf(
                    // Add subscriptions separately
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_monthly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("premium_yearly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
                return@queryProductDetailsAsync
            }

            productDetailsMap.clear()
            productDetailsList.forEach { details ->
                productDetailsMap[details.productId] = details
                Log.d(TAG, "Product loaded: ${details.productId} - ${details.name} - ${details.oneTimePurchaseOfferDetails?.formattedPrice ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice}")
            }

            listener.onProductsUpdated(productDetailsList)
        }
    }

    fun queryPurchases() {
        if (!isServiceConnected) return

        Log.d(TAG, "Querying existing purchases")

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Query purchases failed: ${billingResult.debugMessage}")
                return@queryPurchasesAsync
            }

            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var hasPremiumAccess = false

        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                continue
            }

            val isPremiumPurchase = purchase.products.any { it in PREMIUM_PRODUCT_IDS }
            if (!isPremiumPurchase) {
                continue
            }

            hasPremiumAccess = true

            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }

        setHasPremiumAccess(hasPremiumAccess)
        listener.onRestoreCompleted(hasPremiumAccess)
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged: ${purchase.purchaseToken}")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (!isServiceConnected) {
            Log.w(TAG, "Launch blocked: billing service disconnected")
            listener.onPurchaseError(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                "Billing service not connected"
            )
            return
        }

        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            Log.w(TAG, "Launch blocked: product unavailable: $productId")
            listener.onPurchaseError(
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                "Product not found: $productId"
            )
            return
        }

        // Generate purchase attempt ID for correlation
        val purchaseAttemptId = "${System.currentTimeMillis()}_${productId}"
        purchaseAttemptIds[productId] = purchaseAttemptId

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    // For subscriptions, set the offer token
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
                        setOfferToken(offer.offerToken)
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        Log.d(TAG, "Launching billing flow for: $productId")

        billingClient.launchBillingFlow(activity, billingFlowParams)
            .let { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    listener.onPurchaseError(result.responseCode, result.debugMessage)
                }
            }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val localPurchases = purchases ?: emptyList()
                Log.d(TAG, "Purchase update success; count=${localPurchases.size}")
                handlePurchases(localPurchases)

                localPurchases.forEach { purchase ->
                    purchase.products.forEach { productId ->
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
                Log.e(TAG, "Purchase failed: ${billingResult.responseCode}, ${billingResult.debugMessage}")
                listener.onPurchaseError(billingResult.responseCode, billingResult.debugMessage)
            }
        }
    }

    fun restorePurchases() {
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
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
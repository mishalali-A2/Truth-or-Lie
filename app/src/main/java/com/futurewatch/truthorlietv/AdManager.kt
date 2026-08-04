package com.futurewatch.truthorlietv

import android.app.Activity
import android.util.Log
import com.unity3d.ads.UnityAds
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import android.os.Handler
import android.os.Looper
import android.content.Context
import com.futurewatch.truthorlietv.analytics.AdAnalyticsTracker

object AdManager {
    private const val REWARDED_ID = "Rewarded_Android"
    private const val INTERSTITIAL_ID = "Interstitial_Android"
    private const val GAME_ID = "6069840"

    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN = 180 * 1000L // 3 minutes in milliseconds
    private var isInterstitialPreloaded = false
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var isInitialized = false
    private var isShowingInterstitial = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(activity: Activity, testMode: Boolean = false, onComplete: () -> Unit = {}) {
        if (isInitialized) {
            onComplete()
            return
        }

        UnityAds.initialize(activity, GAME_ID, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d("AdManager", "✅ Unity Ads initialized successfully")
                isInitialized = true
                AdAnalyticsTracker.logInitSucceeded()
                preloadInterstitial()
                preloadRewarded()
                onComplete()
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError,
                message: String
            ) {
                Log.e("AdManager", "❌ Unity Ads init failed: $error - $message")
                isInitialized = false
                AdAnalyticsTracker.logInitFailed(error.name, message)
            }
        })
    }

    private fun hasNoAds(context: Context): Boolean {
        val billingPrefs = context.getSharedPreferences("billing_prefs", Context.MODE_PRIVATE)
        val premium = billingPrefs.getBoolean("premium_access", false)

        val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val adsRemoved = appPrefs.getBoolean("ads_removed", false)

        return premium || adsRemoved
    }
    fun preloadRewarded() {
        if (!isInitialized || isLoadingRewarded) return

        isLoadingRewarded = true
        Log.d("AdManager", "Preloading rewarded ad...")
        AdAnalyticsTracker.logLoadRequested(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)

        UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Rewarded ad preloaded successfully")
                isLoadingRewarded = false
                AdAnalyticsTracker.logLoadSucceeded(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "❌ Failed to preload rewarded: $error - $message")
                isLoadingRewarded = false
                AdAnalyticsTracker.logLoadFailed(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED, error.name, message)
            }
        })
    }

    fun preloadInterstitial() {
        if (!isInitialized) return
        if (isLoadingInterstitial || isInterstitialPreloaded) return

        isLoadingInterstitial = true
        Log.d("AdManager", "Preloading interstitial ad...")
        AdAnalyticsTracker.logLoadRequested(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)

        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Interstitial preloaded successfully")
                isInterstitialPreloaded = true
                isLoadingInterstitial = false
                AdAnalyticsTracker.logLoadSucceeded(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "❌ Failed to preload interstitial: $error - $message")
                isLoadingInterstitial = false
                isInterstitialPreloaded = false
                AdAnalyticsTracker.logLoadFailed(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, error.name, message)
            }
        })
    }

    private fun canShowInterstitial(): Boolean {
        if (lastInterstitialTime == 0L) return true
        val diff = System.currentTimeMillis() - lastInterstitialTime
        return diff >= INTERSTITIAL_COOLDOWN
    }

    fun showInterstitial(
        activity: Activity,
        onComplete: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (hasNoAds(activity)) {
            Log.d("AdManager", "🚫 Ads disabled (user purchased remove_ads)")
            onComplete()
            return
        }

        Log.d("AdManager", "=== SHOW INTERSTITIAL CALLED ===")
        Log.d("AdManager", "isInitialized: $isInitialized")
        Log.d("AdManager", "isInterstitialPreloaded: $isInterstitialPreloaded")
        Log.d("AdManager", "canShowInterstitial: ${canShowInterstitial()}")
        Log.d("AdManager", "isShowingInterstitial: $isShowingInterstitial")

        if (!isInitialized) {
            Log.e("AdManager", "Unity Ads not initialized")
            onFailed()
            return
        }

        if (!canShowInterstitial()) {
            Log.d("AdManager", "Interstitial on cooldown")
            AdAnalyticsTracker.logShowSkippedCooldown(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
            onFailed()
            return
        }

        if (isShowingInterstitial) {
            Log.d("AdManager", "Already showing an interstitial")
            onFailed()
            return
        }

        // Pause music
        MusicManager.pauseMusic()

        // If preloaded, show it
        if (isInterstitialPreloaded) {
            Log.d("AdManager", "Preloaded interstitial available, showing now")
            isInterstitialPreloaded = false
            isShowingInterstitial = true
            AdAnalyticsTracker.logShowRequested(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)

            UnityAds.show(activity, INTERSTITIAL_ID, object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    Log.d("AdManager", "Interstitial complete: $state")
                    lastInterstitialTime = System.currentTimeMillis()
                    isShowingInterstitial = false
                    MusicManager.resumeMusic()
                    AdAnalyticsTracker.logShowCompleted(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, state.name)

                    // Preload next one
                    mainHandler.postDelayed({
                        preloadInterstitial()
                    }, 5000)

                    onComplete()
                }

                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    Log.e("AdManager", "Interstitial show failed: $error - $message")
                    isShowingInterstitial = false
                    MusicManager.resumeMusic()
                    AdAnalyticsTracker.logShowFailed(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, error.name, message)

                    // Try to preload again
                    mainHandler.postDelayed({
                        preloadInterstitial()
                    }, 3000)

                    onFailed()
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d("AdManager", "Interstitial started playing")
                    AdAnalyticsTracker.logShowStarted(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d("AdManager", "Interstitial clicked")
                    AdAnalyticsTracker.logShowClicked(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
                }
            })
            return
        }

        // Not preloaded, load then show
        Log.d("AdManager", "No preloaded interstitial, loading on demand...")
        AdAnalyticsTracker.logShowFallbackOnDemand(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)

        if (isLoadingInterstitial) {
            Log.d("AdManager", "Interstitial already loading, waiting...")
            // Wait for it to finish loading
            mainHandler.postDelayed({
                if (isInterstitialPreloaded) {
                    showInterstitial(activity, onComplete, onFailed)
                } else {
                    MusicManager.resumeMusic()
                    onFailed()
                }
            }, 2000)
            return
        }

        isLoadingInterstitial = true
        AdAnalyticsTracker.logLoadRequested(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)

        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Interstitial loaded on demand")
                isLoadingInterstitial = false
                isInterstitialPreloaded = false
                isShowingInterstitial = true
                AdAnalyticsTracker.logLoadSucceeded(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
                AdAnalyticsTracker.logShowRequested(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)

                UnityAds.show(activity, INTERSTITIAL_ID, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.d("AdManager", "Interstitial complete: $state")
                        lastInterstitialTime = System.currentTimeMillis()
                        isShowingInterstitial = false
                        MusicManager.resumeMusic()
                        AdAnalyticsTracker.logShowCompleted(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, state.name)
                        onComplete()
                    }

                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        Log.e("AdManager", "Interstitial show failed: $error - $message")
                        isShowingInterstitial = false
                        MusicManager.resumeMusic()
                        AdAnalyticsTracker.logShowFailed(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, error.name, message)
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.d("AdManager", "Interstitial started playing")
                        AdAnalyticsTracker.logShowStarted(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
                    }

                    override fun onUnityAdsShowClick(placementId: String) {
                        Log.d("AdManager", "Interstitial clicked")
                        AdAnalyticsTracker.logShowClicked(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL)
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "❌ Failed to load interstitial: $error - $message")
                isLoadingInterstitial = false
                MusicManager.resumeMusic()
                AdAnalyticsTracker.logLoadFailed(AdAnalyticsTracker.PLACEMENT_RESULTS_INTERSTITIAL, AdAnalyticsTracker.FORMAT_INTERSTITIAL, error.name, message)
                onFailed()
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        if (hasNoAds(activity)) {
            onFailed()
            return
        }

        Log.d("AdManager", "=== SHOW REWARDED AD CALLED ===")

        if (!isInitialized) {
            Log.e("AdManager", "Unity Ads not initialized")
            onFailed()
            return
        }

        MusicManager.pauseMusic()

        if (isLoadingRewarded) {
            Log.d("AdManager", "Rewarded ad already loading")
            onFailed()
            return
        }

        isLoadingRewarded = true
        Log.d("AdManager", "Loading rewarded ad...")
        AdAnalyticsTracker.logLoadRequested(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)

        UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Rewarded ad loaded, showing...")
                isLoadingRewarded = false
                AdAnalyticsTracker.logLoadSucceeded(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)
                AdAnalyticsTracker.logShowRequested(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)

                UnityAds.show(activity, REWARDED_ID, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.d("AdManager", "Rewarded ad complete: $state")
                        MusicManager.resumeMusic()
                        AdAnalyticsTracker.logShowCompleted(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED, state.name)

                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            Log.d("AdManager", "✅ Reward earned!")
                            AdAnalyticsTracker.logRewardEarned(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED)
                            onRewardEarned()
                        } else {
                            Log.d("AdManager", "❌ Reward NOT earned")
                            AdAnalyticsTracker.logRewardSkipped(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, state.name)
                        }
                    }

                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        Log.e("AdManager", "Rewarded ad show failed: $error - $message")
                        isLoadingRewarded = false
                        MusicManager.resumeMusic()
                        AdAnalyticsTracker.logShowFailed(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED, error.name, message)
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.d("AdManager", "Rewarded ad started")
                        AdAnalyticsTracker.logShowStarted(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)
                    }

                    override fun onUnityAdsShowClick(placementId: String) {
                        Log.d("AdManager", "Rewarded ad clicked")
                        AdAnalyticsTracker.logShowClicked(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED)
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "❌ Failed to load rewarded ad: $error - $message")
                isLoadingRewarded = false
                MusicManager.resumeMusic()
                AdAnalyticsTracker.logLoadFailed(AdAnalyticsTracker.PLACEMENT_CATEGORY_UNLOCK_REWARDED, AdAnalyticsTracker.FORMAT_REWARDED, error.name, message)
                onFailed()
            }
        })
    }

    fun isRewardedReady(): Boolean = isInitialized && !isLoadingRewarded
    fun isInterstitialReady(): Boolean = isInitialized && isInterstitialPreloaded && !isLoadingInterstitial
    fun isInitialized(): Boolean = isInitialized
}
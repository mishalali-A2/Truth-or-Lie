package com.futurewatch.truthorlietv

import android.app.Activity
import android.util.Log
import com.unity3d.ads.UnityAds
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import android.os.Handler
import android.os.Looper

object AdManager {
    private const val REWARDED_ID = "Rewarded_Android"
    private const val INTERSTITIAL_ID = "Interstitial_Android"
    private const val GAME_ID = "6069840"

    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN = 90 * 1000L
    private var isInterstitialPreloaded = false
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var isInitialized = false
    private var isShowingInterstitial = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(activity: Activity, testMode: Boolean = true, onComplete: () -> Unit = {}) {
        if (isInitialized) {
            onComplete()
            return
        }

        UnityAds.initialize(activity, GAME_ID, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d("AdManager", "✅ Unity Ads initialized successfully")
                isInitialized = true
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
            }
        })
    }

    fun preloadRewarded() {
        if (!isInitialized || isLoadingRewarded) return

        isLoadingRewarded = true
        Log.d("AdManager", "Preloading rewarded ad...")

        UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Rewarded ad preloaded successfully")
                isLoadingRewarded = false
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "❌ Failed to preload rewarded: $error - $message")
                isLoadingRewarded = false
            }
        })
    }

    fun preloadInterstitial() {
        if (!isInitialized) return
        if (isLoadingInterstitial || isInterstitialPreloaded) return

        isLoadingInterstitial = true
        Log.d("AdManager", "Preloading interstitial ad...")

        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Interstitial preloaded successfully")
                isInterstitialPreloaded = true
                isLoadingInterstitial = false
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "❌ Failed to preload interstitial: $error - $message")
                isLoadingInterstitial = false
                isInterstitialPreloaded = false
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

            UnityAds.show(activity, INTERSTITIAL_ID, object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    Log.d("AdManager", "Interstitial complete: $state")
                    lastInterstitialTime = System.currentTimeMillis()
                    isShowingInterstitial = false
                    MusicManager.resumeMusic()

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

                    // Try to preload again
                    mainHandler.postDelayed({
                        preloadInterstitial()
                    }, 3000)

                    onFailed()
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d("AdManager", "Interstitial started playing")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d("AdManager", "Interstitial clicked")
                }
            })
            return
        }

        // Not preloaded, load then show
        Log.d("AdManager", "No preloaded interstitial, loading on demand...")

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

        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Interstitial loaded on demand")
                isLoadingInterstitial = false
                isInterstitialPreloaded = false
                isShowingInterstitial = true

                UnityAds.show(activity, INTERSTITIAL_ID, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.d("AdManager", "Interstitial complete: $state")
                        lastInterstitialTime = System.currentTimeMillis()
                        isShowingInterstitial = false
                        MusicManager.resumeMusic()
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
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.d("AdManager", "Interstitial started playing")
                    }

                    override fun onUnityAdsShowClick(placementId: String) {
                        Log.d("AdManager", "Interstitial clicked")
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
                onFailed()
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
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

        UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "✅ Rewarded ad loaded, showing...")
                isLoadingRewarded = false

                UnityAds.show(activity, REWARDED_ID, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.d("AdManager", "Rewarded ad complete: $state")
                        MusicManager.resumeMusic()

                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            Log.d("AdManager", "✅ Reward earned!")
                            onRewardEarned()
                        } else {
                            Log.d("AdManager", "❌ Reward NOT earned")
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
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.d("AdManager", "Rewarded ad started")
                    }

                    override fun onUnityAdsShowClick(placementId: String) {
                        Log.d("AdManager", "Rewarded ad clicked")
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
                onFailed()
            }
        })
    }

    fun isRewardedReady(): Boolean = isInitialized && !isLoadingRewarded
    fun isInterstitialReady(): Boolean = isInitialized && isInterstitialPreloaded && !isLoadingInterstitial
    fun isInitialized(): Boolean = isInitialized
}
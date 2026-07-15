package com.futurewatch.truthorlietv.analytics

/**
 * Thin wrapper functions for instrumenting AdManager's existing Unity Ads callbacks. AdManager
 * is the ONLY ad SDK integration in this app (Unity Ads) and exposes exactly two reachable
 * placements:
 *  - [PLACEMENT_RESULTS_INTERSTITIAL] ("Interstitial_Android") shown in FinalResultsActivity.
 *  - [PLACEMENT_CATEGORY_UNLOCK_REWARDED] ("Rewarded_Android") shown from CategoriesActivity's
 *    unlock overlay.
 *
 * No AdMob/mediation/paid-event (impression-level revenue) callback exists in the current Unity
 * Ads integration (no IUnityAdsImpressionListener wired), so ad REVENUE analytics is not
 * possible from this codebase as-is — see ANALYTICS_REPORT.md "Gaps".
 *
 * Unity's raw error enums/messages are sanitized into a small controlled bucket set via
 * [sanitizeLoadError]/[sanitizeShowError] rather than passed through raw, so error_category stays
 * a low-cardinality, BigQuery-friendly dimension.
 */
object AdAnalyticsTracker {

    const val PLACEMENT_RESULTS_INTERSTITIAL = "results_interstitial"
    const val PLACEMENT_CATEGORY_UNLOCK_REWARDED = "category_unlock_rewarded"

    const val FORMAT_INTERSTITIAL = "interstitial"
    const val FORMAT_REWARDED = "rewarded"

    const val SOURCE_UNITY_ADS = "unity_ads"

    // ---- Init ----

    fun logInitSucceeded() {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_INIT_SUCCEEDED,
            mapOf(AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS)
        )
    }

    fun logInitFailed(rawError: String?, rawMessage: String?) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_INIT_FAILED,
            mapOf(
                AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS,
                AnalyticsParams.ERROR_CATEGORY to sanitizeGenericError(rawError, rawMessage)
            )
        )
    }

    // ---- Load ----

    fun logLoadRequested(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_LOAD_REQUESTED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS
            )
        )
    }

    fun logLoadSucceeded(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_LOAD_SUCCEEDED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS
            )
        )
    }

    fun logLoadFailed(placement: String, format: String, rawError: String?, rawMessage: String?) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_LOAD_FAILED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS,
                AnalyticsParams.ERROR_CATEGORY to sanitizeLoadError(rawError, rawMessage)
            )
        )
    }

    // ---- Show ----

    fun logShowRequested(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_REQUESTED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS
            )
        )
    }

    /** Distinct outcome: interstitial show blocked by the 3-minute cooldown (never reached Unity SDK). */
    fun logShowSkippedCooldown(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_SKIPPED_COOLDOWN,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format
            )
        )
    }

    /** Distinct outcome: preload wasn't ready, falling back to an on-demand load-then-show path. */
    fun logShowFallbackOnDemand(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_FALLBACK_ON_DEMAND,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_PRELOADED to false
            )
        )
    }

    fun logShowStarted(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_STARTED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format
            )
        )
    }

    fun logShowClicked(placement: String, format: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_CLICKED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format
            )
        )
    }

    /** [completionState] is Unity's UnityAdsShowCompletionState.name (e.g. "COMPLETED", "SKIPPED"). */
    fun logShowCompleted(placement: String, format: String, completionState: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_COMPLETED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_OUTCOME to completionState.lowercase()
            )
        )
    }

    fun logShowFailed(placement: String, format: String, rawError: String?, rawMessage: String?) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_SHOW_FAILED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_FORMAT to format,
                AnalyticsParams.AD_SOURCE to SOURCE_UNITY_ADS,
                AnalyticsParams.ERROR_CATEGORY to sanitizeShowError(rawError, rawMessage)
            )
        )
    }

    // ---- Reward (rewarded placement only) ----

    fun logRewardEarned(placement: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_REWARD_EARNED,
            mapOf(AnalyticsParams.AD_PLACEMENT to placement)
        )
    }

    fun logRewardSkipped(placement: String, completionState: String) {
        AnalyticsService.logEvent(
            AnalyticsEvents.AD_REWARD_SKIPPED,
            mapOf(
                AnalyticsParams.AD_PLACEMENT to placement,
                AnalyticsParams.AD_OUTCOME to completionState.lowercase()
            )
        )
    }

    // ---- Error sanitization: raw Unity enum/message -> small controlled bucket set ----

    private val BUCKETS = listOf(
        "no_fill", "network", "timeout", "invalid_request", "not_ready", "internal", "callback", "unknown"
    )

    fun sanitizeLoadError(rawError: String?, rawMessage: String?): String {
        val text = "${rawError.orEmpty()} ${rawMessage.orEmpty()}".lowercase()
        return when {
            text.contains("no_fill") || text.contains("nofill") -> "no_fill"
            text.contains("network") || text.contains("connect") -> "network"
            text.contains("timeout") -> "timeout"
            text.contains("invalid") -> "invalid_request"
            text.contains("internal") -> "internal"
            text.isBlank() -> "unknown"
            else -> "unknown"
        }
    }

    fun sanitizeShowError(rawError: String?, rawMessage: String?): String {
        val text = "${rawError.orEmpty()} ${rawMessage.orEmpty()}".lowercase()
        return when {
            text.contains("not_ready") || text.contains("notready") -> "not_ready"
            text.contains("timeout") -> "timeout"
            text.contains("network") -> "network"
            text.contains("internal") -> "internal"
            text.contains("callback") -> "callback"
            text.isBlank() -> "unknown"
            else -> "unknown"
        }
    }

    private fun sanitizeGenericError(rawError: String?, rawMessage: String?): String {
        val text = "${rawError.orEmpty()} ${rawMessage.orEmpty()}".lowercase()
        return when {
            text.contains("network") -> "network"
            text.contains("timeout") -> "timeout"
            text.contains("internal") -> "internal"
            text.isBlank() -> "unknown"
            else -> "unknown"
        }
    }
}

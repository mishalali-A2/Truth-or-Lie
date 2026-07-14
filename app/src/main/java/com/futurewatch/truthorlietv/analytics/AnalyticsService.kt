package com.futurewatch.truthorlietv.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin singleton wrapper around [FirebaseAnalytics]. This is the ONLY place in the app that
 * touches the Firebase Analytics SDK directly — every other file goes through
 * [logEvent]/[setUserProperty]/[setScreen] (or one of the tracker helpers built on top of them).
 *
 * Contract:
 *  - [init] must be called once, from [com.futurewatch.truthorlietv.TruthOrLieApplication.onCreate].
 *  - Every public method NEVER throws: all work is wrapped in try/catch, exceptions are logged via
 *    Log.e and swallowed. An analytics failure must never crash or otherwise affect the app.
 *  - Every public method is safe to call before [init] (calls are simply no-ops, logged at debug
 *    level) and safe to call from any thread — no synchronous I/O happens here; Bundle
 *    construction is cheap in-memory work and FirebaseAnalytics#logEvent is itself async/non-blocking.
 *  - Validation/sanitization of names and values is delegated to [AnalyticsValidator], which is
 *    pure Kotlin (no Android types) so it can be unit tested directly.
 */
object AnalyticsService {

    private const val TAG = "AnalyticsService"

    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    /**
     * Initializes the wrapped FirebaseAnalytics instance. Safe to call multiple times (idempotent).
     * Must be called with the Application context.
     */
    fun init(context: Context) {
        if (firebaseAnalytics != null) return
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            Log.d(TAG, "AnalyticsService initialized")
        } catch (e: Exception) {
            // If this ever fails (e.g. no google-services.json applied / misconfigured project),
            // the app must still function normally — analytics is best-effort only.
            Log.e(TAG, "Failed to initialize FirebaseAnalytics: ${e.message}")
        }
    }

    /**
     * Logs a GA4 event with optional parameters. `params` values may be String, Boolean, Int,
     * Long, Short, Float, or Double — anything else is coerced via toString(). Null/blank values
     * are dropped. Never throws.
     */
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        try {
            val analytics = firebaseAnalytics
            if (analytics == null) {
                Log.d(TAG, "logEvent('$name') skipped — not initialized yet")
                return
            }

            val safeName = AnalyticsValidator.sanitizeEventName(name)
            if (safeName == null) {
                Log.e(TAG, "logEvent: '$name' sanitized to empty/invalid — dropping event")
                return
            }

            val safeParams = AnalyticsValidator.sanitizeParams(params)
            val bundle = toBundle(safeParams)

            analytics.logEvent(safeName, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "logEvent('$name') failed: ${e.message}")
        }
    }

    /**
     * Sets a GA4 user property. Never throws. `value` is truncated/sanitized the same way a
     * string parameter value is.
     */
    fun setUserProperty(name: String, value: String?) {
        try {
            val analytics = firebaseAnalytics ?: run {
                Log.d(TAG, "setUserProperty('$name') skipped — not initialized yet")
                return
            }

            val safeName = AnalyticsValidator.sanitizeParamName(name)
            if (safeName == null) {
                Log.e(TAG, "setUserProperty: '$name' sanitized to empty/invalid — dropping")
                return
            }

            val safeValue = (AnalyticsValidator.sanitizeParamValue(value) as?
                AnalyticsValidator.ParamValue.StringValue)?.value

            analytics.setUserProperty(safeName, safeValue)
        } catch (e: Exception) {
            Log.e(TAG, "setUserProperty('$name') failed: ${e.message}")
        }
    }

    /**
     * Records a screen_view style event via the standard GA4 screen tracking call. Kept separate
     * from [logEvent] because Firebase treats screen tracking specially (setCurrentScreen /
     * screen_view semantics via a Bundle on logEvent in modern SDKs).
     */
    fun setScreen(screenName: String, screenClass: String? = null) {
        try {
            val analytics = firebaseAnalytics ?: run {
                Log.d(TAG, "setScreen('$screenName') skipped — not initialized yet")
                return
            }

            val safeName = AnalyticsValidator.sanitizeEventName(screenName) ?: return

            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, safeName)
                if (screenClass != null) {
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass.take(100))
                }
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "setScreen('$screenName') failed: ${e.message}")
        }
    }

    private fun toBundle(params: Map<String, AnalyticsValidator.ParamValue>): Bundle {
        val bundle = Bundle()
        for ((key, value) in params) {
            when (value) {
                is AnalyticsValidator.ParamValue.StringValue -> bundle.putString(key, value.value)
                is AnalyticsValidator.ParamValue.LongValue -> bundle.putLong(key, value.value)
                is AnalyticsValidator.ParamValue.DoubleValue -> bundle.putDouble(key, value.value)
                is AnalyticsValidator.ParamValue.BoolValue -> bundle.putString(key, value.value.toString())
            }
        }
        return bundle
    }

    /** Test/debug only — allows tests to verify init state without touching Firebase. */
    internal fun isInitializedForTest(): Boolean = firebaseAnalytics != null
}

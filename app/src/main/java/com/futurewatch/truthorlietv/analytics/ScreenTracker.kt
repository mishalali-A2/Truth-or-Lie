package com.futurewatch.truthorlietv.analytics

import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Reusable screen-duration + screen_enter/screen_exit tracker, one instance per Activity.
 *
 * Usage (2 lines, added to onCreate of every Activity):
 * ```
 * private val screenTracker = ScreenTracker(AnalyticsScreens.MAIN, previousScreen = null)
 * override fun onCreate(...) { ...; lifecycle.addObserver(screenTracker) }
 * ```
 * A `DefaultLifecycleObserver` registered on the Activity's own `lifecycle` was chosen over
 * manual onResume/onPause overrides because it requires touching only onCreate in each of the 14
 * activities (one field + one addObserver call) instead of duplicating onResume/onPause overrides
 * everywhere — less boilerplate, and it can't be forgotten to be paired correctly since
 * ON_RESUME/ON_PAUSE are always delivered by the platform in order.
 *
 * Dedup: guards against onResume firing again without a real screen change (e.g. a transient
 * dialog re-resuming the Activity) — [onStart]/ON_RESUME only starts a new duration timer if one
 * isn't already running; it does not stop+restart.
 *
 * Duration: measured with [SystemClock.elapsedRealtime] (monotonic, immune to wall-clock changes).
 *
 * Backgrounding: hooks [ProcessLifecycleOwner] so that if the whole app is backgrounded while this
 * screen is on top, screen_exit is still flushed with exit_reason=background and the correct
 * duration, instead of silently losing the exit event (ON_PAUSE of the Activity's own lifecycle
 * does fire in that case too, so in practice this is a safety net / documents intent — see
 * exit_reason=navigation vs background distinction in the emitted event).
 *
 * Instance-per-launch: each Activity relaunch (e.g. VotingActivity relaunching itself per player
 * turn) creates a brand new Activity instance with a brand new ScreenTracker — that is intentional
 * and NOT coalesced; each relaunch is a legitimate distinct screen view.
 */
class ScreenTracker(
    private val screenName: String,
    private val previousScreen: String? = null
) : DefaultLifecycleObserver {

    private var enterTimeMs: Long = 0L
    private var isActive: Boolean = false
    private var hasLoggedEnter: Boolean = false

    private val appBackgroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // App-level background while this screen is the active one on its own lifecycle.
            if (isActive) {
                flushExit(exitReason = "background")
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isActive) return // dedupe: already tracking, ignore a spurious re-entrant onStart
        isActive = true
        enterTimeMs = SystemClock.elapsedRealtime()

        if (!hasLoggedEnter) {
            hasLoggedEnter = true
            AnalyticsService.logEvent(
                AnalyticsEvents.SCREEN_ENTER,
                mapOf(
                    AnalyticsParams.SCREEN_NAME to screenName,
                    AnalyticsParams.PREVIOUS_SCREEN to previousScreen
                )
            )
        }

        // Listen for whole-app backgrounding while we're the foreground screen.
        ProcessLifecycleOwner.get().lifecycle.addObserver(appBackgroundObserver)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!isActive) return
        flushExit(exitReason = "navigation")
        try {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(appBackgroundObserver)
        } catch (_: Exception) {
            // Lifecycle may already be destroyed — safe to ignore, nothing left to clean up.
        }
    }

    private fun flushExit(exitReason: String) {
        if (!isActive) return
        isActive = false
        val durationMs = SystemClock.elapsedRealtime() - enterTimeMs
        AnalyticsService.logEvent(
            AnalyticsEvents.SCREEN_EXIT,
            mapOf(
                AnalyticsParams.SCREEN_NAME to screenName,
                AnalyticsParams.SCREEN_DURATION_MS to durationMs,
                AnalyticsParams.EXIT_REASON to exitReason
            )
        )
    }

    companion object {
        /**
         * Convenience for the common case: create a tracker and register it with the activity's
         * lifecycle in one call. Also calls [AnalyticsService.setScreen] for GA4's standard
         * screen_view reporting alongside the custom screen_enter/screen_exit pair.
         */
        fun attach(activity: Activity, screenName: String, previousScreen: String? = null): ScreenTracker {
            val tracker = ScreenTracker(screenName, previousScreen)
            if (activity is androidx.lifecycle.LifecycleOwner) {
                activity.lifecycle.addObserver(tracker)
            }
            AnalyticsService.setScreen(screenName, activity.javaClass.simpleName)
            return tracker
        }
    }
}

/** Stable screen_name constants — one per Activity, used by every ScreenTracker.attach() call site. */
object AnalyticsScreens {
    const val SPLASH = "splash"
    const val MAIN = "main"
    const val CATEGORIES = "categories"
    const val ROUNDS = "rounds"
    const val PLAYER_COUNT = "player_count"
    const val PLAYER_NAMES = "player_names"
    const val FACTS = "facts"
    const val VOTING = "voting"
    const val RESULTS = "results"
    const val FINAL_RESULTS = "final_results"
    const val LEADERBOARD = "leaderboard"
    const val HOW_TO_PLAY = "how_to_play"
    const val SETTINGS = "settings"
    const val PURCHASE = "purchase"
}

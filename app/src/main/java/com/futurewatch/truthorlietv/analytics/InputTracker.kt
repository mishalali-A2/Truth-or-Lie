package com.futurewatch.truthorlietv.analytics

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * D-pad / focus event helper with two independent throttling strategies:
 *
 * 1. KEY THROTTLE (for VotingActivity's onKeyDown Truth/Lie/pause/lock D-pad handling):
 *    time-window dedup — an identical (control_id, action) pair repeated within
 *    [KEY_THROTTLE_WINDOW_MS] (200ms) is suppressed, UNLESS the control actually changed (a
 *    genuine A->B transition always passes) or the action is a "final" one (select/lock/back),
 *    which always passes through regardless of timing since those are meaningful, low-frequency,
 *    already rate-limited by VotingActivity's own isLocked/isSelectionAnimating animation guards.
 *    200ms was chosen because VotingActivity's own selection animation lock is 300ms — a shorter
 *    window here means the key throttle almost never needs to fire in practice (the app's own
 *    state guards already prevent re-entrant presses), it exists purely as defense-in-depth
 *    against any future guard regression.
 *
 * 2. FOCUS IDLE-GAP AGGREGATION (for CategoriesActivity's 15-card grid and LeaderboardActivity's
 *    unbounded row list): rather than emitting an event on every transient focus step while a
 *    user pans across a grid/list with the D-pad, this aggregates into a single
 *    [AnalyticsEvents.FOCUS_SETTLED] event fired [FOCUS_IDLE_WINDOW_MS] (500ms) after the LAST
 *    focus change in a burst — i.e. debounce, not throttle. Each new focus change during the
 *    window resets the timer. This bounds volume to at most one event per ~500ms of continuous
 *    D-pad panning, and exactly one event when the user stops on a card/row.
 *
 * The pure decision logic (no Android dependency, so testable on the JVM) lives in
 * [KeyThrottle] and [FocusIdleAggregator.shouldEmit]; the Handler-based debounce scheduling used
 * for the real UI lives in [FocusIdleAggregator.onFocusChanged].
 */
object InputTracker {

    const val KEY_THROTTLE_WINDOW_MS = 200L
    const val FOCUS_IDLE_WINDOW_MS = 500L

    /** Actions that always pass the key throttle regardless of timing (final/committing actions). */
    val ALWAYS_PASS_ACTIONS = setOf("select", "lock", "back", "pause", "resume")

    /**
     * Pure decision logic for whether a repeated D-pad key action should be suppressed.
     * No Android dependency — directly unit testable.
     */
    class KeyThrottle(private val windowMs: Long = KEY_THROTTLE_WINDOW_MS) {
        private var lastControlId: String? = null
        private var lastAction: String? = null
        private var lastTimestampMs: Long = -1L

        /**
         * Returns true if this (controlId, action) at [nowMs] should be logged, false if it
         * should be suppressed as a duplicate repeat within the throttle window.
         * Always call this once per candidate event — it has side effects (updates last-seen state).
         */
        fun shouldEmit(controlId: String, action: String, nowMs: Long): Boolean {
            val isAlwaysPass = action in ALWAYS_PASS_ACTIONS
            val isSameAsLast = controlId == lastControlId && action == lastAction
            val withinWindow = lastTimestampMs >= 0 && (nowMs - lastTimestampMs) < windowMs

            val suppressed = isSameAsLast && withinWindow && !isAlwaysPass

            lastControlId = controlId
            lastAction = action
            lastTimestampMs = nowMs

            return !suppressed
        }
    }

    /**
     * Pure idle-gap decision helper for focus aggregation, exposed separately from the Handler
     * scheduling so the "should a settle event fire after N ms of inactivity" logic is testable
     * without needing a real Looper.
     */
    object FocusIdleLogic {
        /** True if [elapsedSinceLastChangeMs] indicates the burst has gone idle and a settle event is due. */
        fun isIdle(elapsedSinceLastChangeMs: Long, idleWindowMs: Long = FOCUS_IDLE_WINDOW_MS): Boolean {
            return elapsedSinceLastChangeMs >= idleWindowMs
        }
    }

    /**
     * Android-facing debounced aggregator for a single grid/list (e.g. one instance for
     * CategoriesActivity's card grid, one instance for LeaderboardActivity's row list). Each call
     * to [onFocusChanged] resets a 500ms timer; when the timer elapses without another call, a
     * single [AnalyticsEvents.FOCUS_SETTLED] event is emitted for whichever control was focused
     * last.
     */
    class FocusIdleAggregator(private val screenName: String) {
        private val handler = Handler(Looper.getMainLooper())
        private var pendingControlId: String? = null

        private val settleRunnable = Runnable {
            val controlId = pendingControlId
            if (controlId != null) {
                AnalyticsService.logEvent(
                    AnalyticsEvents.FOCUS_SETTLED,
                    mapOf(
                        AnalyticsParams.SCREEN_NAME to screenName,
                        AnalyticsParams.CONTROL_ID to controlId
                    )
                )
            }
        }

        fun onFocusChanged(controlId: String) {
            pendingControlId = controlId
            handler.removeCallbacks(settleRunnable)
            handler.postDelayed(settleRunnable, FOCUS_IDLE_WINDOW_MS)
        }

        /** Cancel any pending settle event, e.g. when the Activity is being destroyed/paused. */
        fun cancel() {
            handler.removeCallbacks(settleRunnable)
            pendingControlId = null
        }
    }

    // ---- Low-frequency, standalone focus tracking (buttons everywhere except the two grids) ----

    /** Direct (non-aggregated) focus_changed event for standalone controls — safe to fire individually. */
    fun logFocusChanged(screenName: String, controlId: String, hasFocus: Boolean) {
        if (!hasFocus) return // only log the "gained focus" transition, not "lost focus" — halves volume, no info loss
        AnalyticsService.logEvent(
            AnalyticsEvents.FOCUS_CHANGED,
            mapOf(
                AnalyticsParams.SCREEN_NAME to screenName,
                AnalyticsParams.CONTROL_ID to controlId
            )
        )
    }

    // ---- D-pad key action logging (VotingActivity) ----

    private val votingKeyThrottle = KeyThrottle()

    /**
     * Logs a D-pad key action in VotingActivity, respecting the key throttle. Call this
     * alongside (not instead of) the existing isLocked/isSelectionAnimating guards — this
     * function does not know about game state, it only prevents duplicate analytics events for
     * rapid repeats of the identical action.
     */
    fun logVotingKeyAction(controlId: String, action: String) {
        val now = SystemClock.elapsedRealtime()
        if (!votingKeyThrottle.shouldEmit(controlId, action, now)) return

        AnalyticsService.logEvent(
            AnalyticsEvents.DPAD_KEY_ACTION,
            mapOf(
                AnalyticsParams.SCREEN_NAME to AnalyticsScreens.VOTING,
                AnalyticsParams.CONTROL_ID to controlId,
                AnalyticsParams.INPUT_ACTION to action
            )
        )
    }
}

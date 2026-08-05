package com.futurewatch.truthorlietv.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for [InputTracker]'s throttle/aggregation decision logic
 * ([InputTracker.KeyThrottle] and [InputTracker.FocusIdleLogic]). No Handler/Looper needed since
 * these are exposed as plain functions independent of the Android-facing scheduling wrapper.
 */
class InputTrackerTest {

    // ---- KeyThrottle: time-window dedup for repeated identical (control, action) pairs ----

    @Test
    fun `first event for a control always emits`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 0L))
    }

    @Test
    fun `identical control and action repeated within window is suppressed`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 0L))
        // Same control+action, only 50ms later — within the 200ms window — should be suppressed.
        assertFalse(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 50L))
    }

    @Test
    fun `identical control and action repeated after window passes emits again`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 0L))
        // 250ms later — outside the 200ms window — should emit again.
        assertTrue(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 250L))
    }

    @Test
    fun `genuine control-to-control transition is never suppressed even when fast`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 0L))
        // Different control (truth -> lie), only 10ms later — must NOT be suppressed.
        assertTrue(throttle.shouldEmit("voting_answer_lie", "dpad_right", nowMs = 10L))
    }

    @Test
    fun `select action always passes through regardless of timing`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_lock_answer", "select", nowMs = 0L))
        // Same control+action immediately after — "select" is an always-pass action.
        assertTrue(throttle.shouldEmit("voting_lock_answer", "select", nowMs = 1L))
    }

    @Test
    fun `lock action always passes through regardless of timing`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_lock_answer", "lock", nowMs = 0L))
        assertTrue(throttle.shouldEmit("voting_lock_answer", "lock", nowMs = 5L))
    }

    @Test
    fun `back action always passes through regardless of timing`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("categories_back", "back", nowMs = 0L))
        assertTrue(throttle.shouldEmit("categories_back", "back", nowMs = 5L))
    }

    @Test
    fun `pause and resume actions always pass through regardless of timing`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_pause", "pause", nowMs = 0L))
        assertTrue(throttle.shouldEmit("voting_pause", "pause", nowMs = 1L))
        assertTrue(throttle.shouldEmit("voting_pause_resume", "resume", nowMs = 2L))
        assertTrue(throttle.shouldEmit("voting_pause_resume", "resume", nowMs = 3L))
    }

    @Test
    fun `non-repeat different action on same control is not suppressed`() {
        val throttle = InputTracker.KeyThrottle(windowMs = 200L)
        assertTrue(throttle.shouldEmit("voting_answer_truth", "dpad_left", nowMs = 0L))
        // Same control, different action, fast — action changed, so it's not an identical repeat.
        assertTrue(throttle.shouldEmit("voting_answer_truth", "select", nowMs = 5L))
    }

    // ---- FocusIdleLogic: idle-gap aggregation decision for grids/lists ----

    @Test
    fun `focus idle logic reports not idle before the window elapses`() {
        assertFalse(InputTracker.FocusIdleLogic.isIdle(elapsedSinceLastChangeMs = 100L, idleWindowMs = 500L))
    }

    @Test
    fun `focus idle logic reports idle once the window has elapsed`() {
        assertTrue(InputTracker.FocusIdleLogic.isIdle(elapsedSinceLastChangeMs = 500L, idleWindowMs = 500L))
    }

    @Test
    fun `focus idle logic reports idle well past the window`() {
        assertTrue(InputTracker.FocusIdleLogic.isIdle(elapsedSinceLastChangeMs = 900L, idleWindowMs = 500L))
    }
}

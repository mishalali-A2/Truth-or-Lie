package com.futurewatch.truthorlietv.analytics

/**
 * Central catalogue of custom GA4 event name constants used throughout the app.
 *
 * Naming convention: snake_case, <= 40 chars, no firebase_/google_/ga_ prefix (enforced again at
 * runtime by [AnalyticsValidator]). Lifecycle events are deliberately prefixed with `app_` (e.g.
 * [APP_SESSION_START] rather than `session_start`) to avoid colliding with GA4's own automatically
 * collected `session_start` event.
 *
 * See ANALYTICS_REPORT.md at the repo root for the full trigger/param/example-flow catalogue.
 */
object AnalyticsEvents {

    // ---- Lifecycle / session ----
    const val APP_SESSION_START = "app_session_start"
    const val APP_SESSION_END = "app_session_end"
    const val APP_FOREGROUND = "app_foreground"
    const val APP_BACKGROUND = "app_background"

    // ---- Screen ----
    const val SCREEN_ENTER = "screen_enter"
    const val SCREEN_EXIT = "screen_exit"

    // ---- Navigation ----
    const val NAVIGATION = "navigation"
    const val BACK_PRESSED = "back_pressed"

    // ---- Input / D-pad ----
    const val DPAD_KEY_ACTION = "dpad_key_action"

    // ---- Focus ----
    const val FOCUS_CHANGED = "focus_changed"
    const val FOCUS_SETTLED = "focus_settled"

    // ---- Controls (generic button/click taps) ----
    const val CONTROL_CLICK = "control_click"

    // ---- Dialogs / overlays ----
    const val DIALOG_SHOWN = "dialog_shown"
    const val DIALOG_DISMISSED = "dialog_dismissed"

    // ---- Feature / game funnel ----
    const val CATEGORY_SELECTED = "category_selected"
    const val ROUNDS_SELECTED = "rounds_selected"
    const val PLAYER_COUNT_SELECTED = "player_count_selected"
    const val GAME_STARTED = "game_started"
    const val ROUND_STARTED = "round_started"
    const val ANSWER_SUBMITTED = "answer_submitted"
    const val ROUND_COMPLETED = "round_completed"
    const val GAME_COMPLETED = "game_completed"
    const val GAME_ABANDONED = "game_abandoned"

    // ---- Monetization / category unlock ----
    const val CATEGORY_LOCKED_VIEWED = "category_locked_viewed"
    const val CATEGORY_UNLOCK_AD_WATCHED = "category_unlock_ad_watched"
    const val CATEGORY_UNLOCK_PURCHASED = "category_unlock_purchased"
    const val CATEGORY_UNLOCK_EXPIRED = "category_unlock_expired"

    // ---- Purchases / billing ----
    const val PURCHASE_INITIATED = "purchase_initiated"
    const val PURCHASE_SUCCEEDED = "purchase_succeeded"
    const val PURCHASE_FAILED = "purchase_failed"
    const val PURCHASE_CANCELED = "purchase_canceled"
    const val RESTORE_PURCHASES_REQUESTED = "restore_purchases_requested"
    const val RESTORE_PURCHASES_COMPLETED = "restore_purchases_completed"
    const val BILLING_SETUP_FINISHED = "billing_setup_finished"
    const val BILLING_DISCONNECTED = "billing_disconnected"

    // ---- Settings ----
    const val MUSIC_TOGGLED = "music_toggled"
    const val MUSIC_GENRE_CHANGED = "music_genre_changed"
    const val TIMER_DURATION_CHANGED = "timer_duration_changed"
    const val OTHER_APP_CROSS_PROMO_TAPPED = "other_app_cross_promo_tapped"
    const val RATE_APP_TAPPED = "rate_app_tapped"
    const val PRIVACY_POLICY_TAPPED = "privacy_policy_tapped"
    const val TERMS_OF_SERVICE_TAPPED = "terms_of_service_tapped"

    // ---- Ads (Unity Ads — only ad SDK in the project) ----
    const val AD_INIT_SUCCEEDED = "ad_init_succeeded"
    const val AD_INIT_FAILED = "ad_init_failed"
    const val AD_LOAD_REQUESTED = "ad_load_requested"
    const val AD_LOAD_SUCCEEDED = "ad_load_succeeded"
    const val AD_LOAD_FAILED = "ad_load_failed"
    const val AD_SHOW_REQUESTED = "ad_show_requested"
    const val AD_SHOW_STARTED = "ad_show_started"
    const val AD_SHOW_CLICKED = "ad_show_clicked"
    const val AD_SHOW_COMPLETED = "ad_show_completed"
    const val AD_SHOW_FAILED = "ad_show_failed"
    const val AD_REWARD_EARNED = "ad_reward_earned"
    const val AD_REWARD_SKIPPED = "ad_reward_skipped"
    const val AD_SHOW_SKIPPED_COOLDOWN = "ad_show_skipped_cooldown"
    const val AD_SHOW_FALLBACK_ON_DEMAND = "ad_show_fallback_on_demand"

    // ---- Errors (sanitized, safe categories only) ----
    const val ERROR_AD = "error_ad"
    const val ERROR_BILLING = "error_billing"
    const val ERROR_DATA_LOAD = "error_data_load"
}

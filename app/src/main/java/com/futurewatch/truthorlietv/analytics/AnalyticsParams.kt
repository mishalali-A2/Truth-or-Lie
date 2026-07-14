package com.futurewatch.truthorlietv.analytics

/**
 * Central catalogue of custom GA4 parameter name constants used throughout the app.
 *
 * Every constant here is <= 40 chars, alphanumeric+underscore, and does not start with a
 * reserved prefix — enforced again defensively at runtime by [AnalyticsValidator].
 *
 * PII NOTE: there is intentionally no "player_name" constant. Player display names are
 * user-typed free text (often children's names in this family game) and must never be sent to
 * analytics — see ANALYTICS_REPORT.md "Privacy exclusions". Only player_index / player_count /
 * score values are safe.
 */
object AnalyticsParams {

    // ---- Screen / navigation ----
    const val SCREEN_NAME = "screen_name"
    const val PREVIOUS_SCREEN = "previous_screen"
    const val DESTINATION_SCREEN = "destination_screen"
    const val NAVIGATION_SOURCE = "navigation_source"
    const val NAVIGATION_OUTCOME = "navigation_outcome"
    const val EXIT_REASON = "exit_reason"

    // ---- Controls ----
    const val CONTROL_ID = "control_id"
    const val CONTROL_ACTION = "control_action"
    const val PREVIOUS_CONTROL = "previous_control"
    const val DESTINATION_CONTROL = "destination_control"

    // ---- Input / D-pad / focus ----
    const val INPUT_ACTION = "input_action"
    const val INPUT_SOURCE = "input_source"
    const val FOCUS_COUNT = "focus_count"

    // ---- Feature / game funnel ----
    const val FEATURE_ID = "feature_id"
    const val FEATURE_OUTCOME = "feature_outcome"
    const val CATEGORY_ID = "category_id"
    const val ROUND_COUNT = "round_count"
    const val ROUND_NUMBER = "round_number"
    const val PLAYER_COUNT = "player_count"
    const val PLAYER_INDEX = "player_index"
    const val SCORE_BUCKET = "score_bucket"
    const val IS_CORRECT = "is_correct"
    const val IS_TIMEOUT = "is_timeout"
    const val IS_TIE = "is_tie"

    // ---- Monetization ----
    const val PRODUCT_ID = "product_id"
    const val PURCHASE_TYPE = "purchase_type"
    const val PLACEMENT_REASON = "placement_reason"
    const val UNLOCK_METHOD = "unlock_method"

    // ---- Ads ----
    const val AD_PLACEMENT = "ad_placement"
    const val AD_FORMAT = "ad_format"
    const val AD_SOURCE = "ad_source"
    const val AD_OUTCOME = "ad_outcome"
    const val AD_PRELOADED = "ad_preloaded"

    // ---- Errors ----
    const val ERROR_CATEGORY = "error_category"
    const val ERROR_SOURCE = "error_source"

    // ---- Timing ----
    const val SCREEN_DURATION_MS = "screen_duration_ms"
    const val SESSION_DURATION_MS = "session_duration_ms"
    const val LOAD_DURATION_MS = "load_duration_ms"
    const val SHOW_DURATION_MS = "show_duration_ms"

    // ---- Device / app ----
    const val DEVICE_CATEGORY = "device_category"
    const val APP_VERSION = "app_version"
    const val PREMIUM_STATUS = "premium_status"

    // ---- Settings ----
    const val SETTING_ID = "setting_id"
    const val SETTING_VALUE = "setting_value"
    const val APP_ID = "app_id"
}

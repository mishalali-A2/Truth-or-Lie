package com.futurewatch.truthorlietv.analytics

/**
 * Pure, Android-Context-free validation/sanitization logic for GA4-bound analytics data.
 *
 * Kept deliberately free of any Android/Firebase types (no Bundle, no Context) so it can be
 * unit tested with plain JUnit on the JVM. [AnalyticsService] is the only caller and is
 * responsible for turning the sanitized output into a Bundle before calling Firebase.
 *
 * GA4 hard limits this enforces (see Firebase docs "Event names and parameters"):
 *  - Event name: <= 40 chars, alphanumeric + underscore, must start with a letter,
 *    must NOT start with a reserved prefix (firebase_, google_, ga_).
 *  - Parameter name: <= 40 chars, alphanumeric + underscore, must NOT start with a reserved prefix.
 *  - Parameter (string) value: <= 100 chars — longer values are truncated, never dropped.
 *  - Max 25 parameters per event — extra parameters are dropped (first-seen-wins), not fatal.
 */
object AnalyticsValidator {

    const val MAX_EVENT_NAME_LENGTH = 40
    const val MAX_PARAM_NAME_LENGTH = 40
    const val MAX_PARAM_STRING_VALUE_LENGTH = 100
    const val MAX_PARAMS_PER_EVENT = 25

    private val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
    private val NAME_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*$")

    /**
     * Result of validating an event name.
     */
    sealed class NameResult {
        data class Valid(val name: String) : NameResult()
        data class Invalid(val reason: String) : NameResult()
    }

    /**
     * Validates an event name against GA4 rules. Does not mutate/sanitize — callers that want a
     * best-effort sanitized fallback should use [sanitizeEventName] instead. This strict form is
     * primarily useful for tests and for callers that want to reject bad names outright.
     */
    fun validateEventName(name: String): NameResult {
        if (name.isBlank()) return NameResult.Invalid("event name is blank")
        if (name.length > MAX_EVENT_NAME_LENGTH) {
            return NameResult.Invalid("event name exceeds $MAX_EVENT_NAME_LENGTH chars")
        }
        if (hasReservedPrefix(name)) {
            return NameResult.Invalid("event name uses reserved prefix (firebase_/google_/ga_)")
        }
        if (!NAME_REGEX.matches(name)) {
            return NameResult.Invalid("event name must be alphanumeric+underscore, starting with a letter")
        }
        return NameResult.Valid(name)
    }

    /**
     * Best-effort sanitizer for an event name: lowercases, replaces invalid characters with
     * underscore, strips reserved prefixes, and truncates to the length limit. Returns null only
     * if nothing usable remains (e.g. blank input or a name that becomes empty after sanitizing).
     * [AnalyticsService] uses this rather than the strict validator so a slightly malformed
     * internal call site never silently drops an event outright.
     */
    fun sanitizeEventName(rawName: String): String? {
        if (rawName.isBlank()) return null

        var name = rawName.trim()

        // Strip reserved prefixes if present (defense in depth — our own constants shouldn't hit this).
        for (prefix in RESERVED_PREFIXES) {
            if (name.startsWith(prefix, ignoreCase = true)) {
                name = name.removePrefix(prefix)
            }
        }

        // Replace any character that isn't alphanumeric/underscore with underscore.
        name = name.map { c -> if (c.isLetterOrDigit() || c == '_') c else '_' }.joinToString("")

        // Must start with a letter; if it doesn't, prefix with "e_".
        if (name.isEmpty()) return null
        if (!name.first().isLetter()) {
            name = "e_$name"
        }

        if (name.length > MAX_EVENT_NAME_LENGTH) {
            name = name.substring(0, MAX_EVENT_NAME_LENGTH)
        }

        // Trailing underscores from truncation are harmless but tidy them up.
        name = name.trimEnd('_')
        if (name.isEmpty()) return null

        return name
    }

    fun hasReservedPrefix(name: String): Boolean {
        val lower = name.lowercase()
        return RESERVED_PREFIXES.any { lower.startsWith(it) }
    }

    /**
     * Sanitizes a parameter name the same way as an event name (same charset/length rules apply
     * per GA4 docs), returning null if nothing usable remains.
     */
    fun sanitizeParamName(rawName: String): String? = sanitizeEventName(rawName)

    /**
     * A single validated/sanitized parameter value ready for Bundle insertion by the caller.
     * Exactly one of the typed fields is non-null.
     */
    sealed class ParamValue {
        data class StringValue(val value: String) : ParamValue()
        data class LongValue(val value: Long) : ParamValue()
        data class DoubleValue(val value: Double) : ParamValue()
        data class BoolValue(val value: Boolean) : ParamValue()
    }

    /**
     * Sanitizes a single raw parameter value:
     *  - null -> null (caller should drop the param entirely)
     *  - blank string -> null (dropped, consistent with null handling)
     *  - String -> truncated to [MAX_PARAM_STRING_VALUE_LENGTH]
     *  - Boolean -> passed through as a Long (0/1) is NOT done here; GA4 Bundles support boolean
     *    via string "true"/"false" convention in this codebase — kept as BoolValue so the caller
     *    (AnalyticsService) decides the Bundle representation.
     *  - Number types pass through unchanged (Int/Long -> LongValue, Float/Double -> DoubleValue).
     */
    fun sanitizeParamValue(raw: Any?): ParamValue? {
        return when (raw) {
            null -> null
            is String -> {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) null
                else ParamValue.StringValue(trimmed.take(MAX_PARAM_STRING_VALUE_LENGTH))
            }
            is Boolean -> ParamValue.BoolValue(raw)
            is Int -> ParamValue.LongValue(raw.toLong())
            is Long -> ParamValue.LongValue(raw)
            is Short -> ParamValue.LongValue(raw.toLong())
            is Float -> ParamValue.DoubleValue(raw.toDouble())
            is Double -> ParamValue.DoubleValue(raw)
            else -> {
                // Unknown type: fall back to toString(), sanitized like a string.
                val str = raw.toString().trim()
                if (str.isEmpty()) null else ParamValue.StringValue(str.take(MAX_PARAM_STRING_VALUE_LENGTH))
            }
        }
    }

    /**
     * Validates and sanitizes a full parameter map for one event:
     *  - drops null/blank values
     *  - drops params whose sanitized name collides with a reserved prefix or becomes empty
     *  - truncates string values
     *  - enforces the max-25-params-per-event GA4 limit (first-seen-wins iteration order)
     *
     * Returns the sanitized map in insertion order (LinkedHashMap semantics of the input Map).
     */
    fun sanitizeParams(rawParams: Map<String, Any?>): Map<String, ParamValue> {
        val result = LinkedHashMap<String, ParamValue>()
        for ((rawKey, rawValue) in rawParams) {
            if (result.size >= MAX_PARAMS_PER_EVENT) break

            val key = sanitizeParamName(rawKey) ?: continue
            val value = sanitizeParamValue(rawValue) ?: continue

            result[key] = value
        }
        return result
    }
}

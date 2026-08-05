package com.futurewatch.truthorlietv.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for [AnalyticsValidator]. No Android/Robolectric dependency needed since
 * the validator is deliberately free of Context/Bundle types.
 */
class AnalyticsValidatorTest {

    // ---- Event name validation ----

    @Test
    fun `valid event name is accepted`() {
        val result = AnalyticsValidator.validateEventName("category_selected")
        assertTrue(result is AnalyticsValidator.NameResult.Valid)
        assertEquals("category_selected", (result as AnalyticsValidator.NameResult.Valid).name)
    }

    @Test
    fun `blank event name is rejected`() {
        val result = AnalyticsValidator.validateEventName("")
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    @Test
    fun `event name exceeding max length is rejected`() {
        val tooLong = "a".repeat(AnalyticsValidator.MAX_EVENT_NAME_LENGTH + 1)
        val result = AnalyticsValidator.validateEventName(tooLong)
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    @Test
    fun `event name at exactly max length is accepted`() {
        val exact = "a".repeat(AnalyticsValidator.MAX_EVENT_NAME_LENGTH)
        val result = AnalyticsValidator.validateEventName(exact)
        assertTrue(result is AnalyticsValidator.NameResult.Valid)
    }

    @Test
    fun `event name with firebase_ prefix is rejected`() {
        val result = AnalyticsValidator.validateEventName("firebase_campaign")
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    @Test
    fun `event name with google_ prefix is rejected`() {
        val result = AnalyticsValidator.validateEventName("google_ad_click")
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    @Test
    fun `event name with ga_ prefix is rejected`() {
        val result = AnalyticsValidator.validateEventName("ga_session")
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    @Test
    fun `event name with invalid characters is rejected by strict validator`() {
        val result = AnalyticsValidator.validateEventName("category-selected!")
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    @Test
    fun `event name starting with a digit is rejected by strict validator`() {
        val result = AnalyticsValidator.validateEventName("1_category_selected")
        assertTrue(result is AnalyticsValidator.NameResult.Invalid)
    }

    // ---- Event name sanitization (best-effort fallback used by AnalyticsService) ----

    @Test
    fun `sanitizeEventName replaces invalid characters with underscore`() {
        // "category-selected!" -> "category_selected_" after char replacement, then the
        // trailing underscore introduced by "!" is trimmed off by the sanitizer.
        val sanitized = AnalyticsValidator.sanitizeEventName("category-selected!")
        assertEquals("category_selected", sanitized)
    }

    @Test
    fun `sanitizeEventName preserves an interior underscore introduced by replacement`() {
        val sanitized = AnalyticsValidator.sanitizeEventName("category selected")
        assertEquals("category_selected", sanitized)
    }

    @Test
    fun `sanitizeEventName strips reserved prefix`() {
        val sanitized = AnalyticsValidator.sanitizeEventName("firebase_custom_event")
        assertTrue(sanitized != null && !AnalyticsValidator.hasReservedPrefix(sanitized))
    }

    @Test
    fun `sanitizeEventName truncates to max length`() {
        val tooLong = "a".repeat(AnalyticsValidator.MAX_EVENT_NAME_LENGTH + 20)
        val sanitized = AnalyticsValidator.sanitizeEventName(tooLong)
        assertTrue(sanitized != null && sanitized.length <= AnalyticsValidator.MAX_EVENT_NAME_LENGTH)
    }

    @Test
    fun `sanitizeEventName returns null for blank input`() {
        assertNull(AnalyticsValidator.sanitizeEventName(""))
        assertNull(AnalyticsValidator.sanitizeEventName("   "))
    }

    @Test
    fun `sanitizeEventName prefixes a name that does not start with a letter`() {
        val sanitized = AnalyticsValidator.sanitizeEventName("123abc")
        assertTrue(sanitized != null && sanitized.first().isLetter())
    }

    // ---- Param name validation ----

    @Test
    fun `param name exceeding max length is dropped from sanitizeParams`() {
        val tooLong = "p".repeat(AnalyticsValidator.MAX_PARAM_NAME_LENGTH + 5)
        val result = AnalyticsValidator.sanitizeParams(mapOf(tooLong to "value"))
        // sanitizeParamName truncates rather than drops (delegates to sanitizeEventName logic),
        // so the resulting key must never exceed the limit.
        result.keys.forEach { assertTrue(it.length <= AnalyticsValidator.MAX_PARAM_NAME_LENGTH) }
    }

    // ---- Param value sanitization ----

    @Test
    fun `null param value sanitizes to null and is dropped`() {
        assertNull(AnalyticsValidator.sanitizeParamValue(null))
    }

    @Test
    fun `blank string param value sanitizes to null and is dropped`() {
        assertNull(AnalyticsValidator.sanitizeParamValue("   "))
    }

    @Test
    fun `string param value is truncated to max length`() {
        val tooLong = "x".repeat(AnalyticsValidator.MAX_PARAM_STRING_VALUE_LENGTH + 50)
        val result = AnalyticsValidator.sanitizeParamValue(tooLong) as AnalyticsValidator.ParamValue.StringValue
        assertEquals(AnalyticsValidator.MAX_PARAM_STRING_VALUE_LENGTH, result.value.length)
    }

    @Test
    fun `string param value at exactly max length is not truncated`() {
        val exact = "x".repeat(AnalyticsValidator.MAX_PARAM_STRING_VALUE_LENGTH)
        val result = AnalyticsValidator.sanitizeParamValue(exact) as AnalyticsValidator.ParamValue.StringValue
        assertEquals(exact, result.value)
    }

    @Test
    fun `boolean param value passes through unchanged`() {
        val resultTrue = AnalyticsValidator.sanitizeParamValue(true) as AnalyticsValidator.ParamValue.BoolValue
        assertTrue(resultTrue.value)
        val resultFalse = AnalyticsValidator.sanitizeParamValue(false) as AnalyticsValidator.ParamValue.BoolValue
        assertFalse(resultFalse.value)
    }

    @Test
    fun `int param value passes through as long`() {
        val result = AnalyticsValidator.sanitizeParamValue(42) as AnalyticsValidator.ParamValue.LongValue
        assertEquals(42L, result.value)
    }

    @Test
    fun `long param value passes through unchanged`() {
        val result = AnalyticsValidator.sanitizeParamValue(123456789L) as AnalyticsValidator.ParamValue.LongValue
        assertEquals(123456789L, result.value)
    }

    @Test
    fun `double param value passes through unchanged`() {
        val result = AnalyticsValidator.sanitizeParamValue(3.14) as AnalyticsValidator.ParamValue.DoubleValue
        assertEquals(3.14, result.value, 0.0001)
    }

    @Test
    fun `float param value passes through as double`() {
        val result = AnalyticsValidator.sanitizeParamValue(2.5f) as AnalyticsValidator.ParamValue.DoubleValue
        assertEquals(2.5, result.value, 0.0001)
    }

    // ---- Full param map sanitization ----

    @Test
    fun `sanitizeParams drops null values but keeps valid ones`() {
        val result = AnalyticsValidator.sanitizeParams(
            mapOf(
                "category_id" to "science",
                "round_count" to null,
                "player_count" to 4
            )
        )
        assertEquals(2, result.size)
        assertTrue(result.containsKey("category_id"))
        assertFalse(result.containsKey("round_count"))
        assertTrue(result.containsKey("player_count"))
    }

    @Test
    fun `sanitizeParams enforces max 25 params per event`() {
        val manyParams = (1..40).associate { "param_$it" to "value_$it" }
        val result = AnalyticsValidator.sanitizeParams(manyParams)
        assertEquals(AnalyticsValidator.MAX_PARAMS_PER_EVENT, result.size)
    }

    @Test
    fun `sanitizeParams on empty map returns empty map`() {
        val result = AnalyticsValidator.sanitizeParams(emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `hasReservedPrefix is case insensitive`() {
        assertTrue(AnalyticsValidator.hasReservedPrefix("Firebase_test"))
        assertTrue(AnalyticsValidator.hasReservedPrefix("GOOGLE_test"))
        assertFalse(AnalyticsValidator.hasReservedPrefix("category_selected"))
    }
}

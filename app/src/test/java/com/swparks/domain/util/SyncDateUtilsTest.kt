package com.swparks.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDateUtilsTest {
    private val clock = TestClock("2025-10-27T12:00:00Z")

    @Test
    fun isUpdateNeeded_whenNullDate_thenTrue() {
        val result: String? = null
        assertTrue(result.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenLessThanOneDay_sinceLastUpdate_thenFalse() {
        val lastUpdate = "2025-10-27T10:00:00Z"
        assertFalse(lastUpdate.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenExactlyOneDay_sinceLastUpdate_thenFalse() {
        val lastUpdate = "2025-10-26T12:00:00Z"
        assertFalse(lastUpdate.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenMoreThanOneDay_sinceLastUpdate_thenTrue() {
        val lastUpdate = "2025-10-26T11:59:59Z"
        assertTrue(lastUpdate.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenDefaultDate_2025_10_25_thenTrue() {
        val lastUpdate = "2025-10-25T00:00:00Z"
        assertTrue(lastUpdate.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenInvalidIsoString_thenTrue() {
        val invalidDate = "not-a-date"
        assertTrue(invalidDate.isUpdateNeeded(clock))
    }

    @Test
    fun isUpdateNeeded_whenEmptyString_thenTrue() {
        val emptyDate = ""
        assertTrue(emptyDate.isUpdateNeeded(clock))
    }
}

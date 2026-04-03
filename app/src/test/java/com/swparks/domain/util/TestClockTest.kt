package com.swparks.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TestClockTest {
    @Test
    fun now_whenFixedInstantSet_thenReturnsFixedTime() {
        val fixedInstant = java.time.Instant.parse("2025-10-25T10:00:00Z")
        val testClock = TestClock(fixedInstant)

        val result = testClock.now()

        assertEquals(fixedInstant, result)
    }

    @Test
    fun nowIsoString_whenFixedInstantSet_thenReturnsCorrectIsoFormat() {
        val fixedInstant = java.time.Instant.parse("2025-10-25T10:00:00Z")
        val testClock = TestClock(fixedInstant)

        val result = testClock.nowIsoString()

        assertEquals("2025-10-25T10:00:00", result)
    }

    @Test
    fun constructor_whenIsoStringPassed_thenParsesCorrectly() {
        val testClock = TestClock("2025-10-25T10:00:00Z")

        val result = testClock.now()

        assertNotNull(result)
        assertEquals(java.time.Instant.parse("2025-10-25T10:00:00Z"), result)
    }
}

package com.swparks.domain.util

import java.time.Instant

/**
 * Abstraction for time operations to enable deterministic testing.
 *
 * Production implementation: [com.swparks.data.util.SystemClock]
 * Test implementation: [com.swparks.domain.util.TestClock]
 */
interface Clock {
    /**
     * Returns the current instant
     */
    fun now(): Instant

    /**
     * Returns the current instant as ISO 8601 formatted string
     */
    fun nowIsoString(): String
}

package com.swparks.domain.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Test implementation of [Clock] with fixed time.
 */
class TestClock(private val fixedInstant: Instant) : Clock {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    constructor(fixedIsoString: String) : this(Instant.parse(fixedIsoString))

    override fun now(): Instant = fixedInstant

    override fun nowIsoString(): String {
        return fixedInstant
            .atZone(ZoneOffset.UTC)
            .format(formatter)
    }
}
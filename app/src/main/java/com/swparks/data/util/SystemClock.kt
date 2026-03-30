package com.swparks.data.util

import com.swparks.domain.util.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Production implementation of [Clock] using system time.
 */
class SystemClock : Clock {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    override fun now(): Instant = Instant.now()

    override fun nowIsoString(): String {
        return Instant.now()
            .atZone(ZoneOffset.UTC)
            .format(formatter)
    }
}
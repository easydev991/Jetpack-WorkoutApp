package com.swparks.domain.util

import java.time.Duration
import java.time.Instant

private val SYNC_INTERVAL = Duration.ofDays(1)

/**
 * Checks if data update is needed based on last sync date.
 *
 * @param clock clock to get current time
 * @return true if update is needed (null date, invalid date, or >1 day since last sync)
 */
fun String?.isUpdateNeeded(clock: Clock): Boolean {
    if (this == null) return true
    val lastUpdate = runCatching { Instant.parse(this) }.getOrNull()
        ?: return true
    val now = clock.now()
    val timeSinceUpdate = Duration.between(lastUpdate, now)
    return timeSinceUpdate > SYNC_INTERVAL
}
package com.swparks.analytics

interface AnalyticsProvider {
    fun log(event: AnalyticsEvent)
}

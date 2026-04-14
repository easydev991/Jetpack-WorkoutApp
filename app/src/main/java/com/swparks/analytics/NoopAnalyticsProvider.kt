package com.swparks.analytics

class NoopAnalyticsProvider : AnalyticsProvider {
    override fun log(event: AnalyticsEvent) = Unit
}

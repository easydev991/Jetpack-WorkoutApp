package com.swparks.util

class FakeAnalyticsReporter : AnalyticsReporter {
    override fun logScreenView(
        screenName: String,
        screenClass: String?
    ) {
    }
}

package com.swparks.analytics

class FakeAnalyticsProvider : AnalyticsProvider {
    private val _events = mutableListOf<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events.toList()

    override fun log(event: AnalyticsEvent) {
        _events.add(event)
    }
}

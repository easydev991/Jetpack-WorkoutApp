package com.swparks.analytics

fun fakeAnalyticsLogger(events: MutableList<AnalyticsEvent> = mutableListOf()): (AnalyticsEvent) -> Unit = { events.add(it) }

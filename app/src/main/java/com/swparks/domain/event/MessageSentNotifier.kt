package com.swparks.domain.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MessageSentNotifier {
    private val _messageSent = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1)
    val messageSent: SharedFlow<Long> = _messageSent.asSharedFlow()

    fun notifyMessageSent(userId: Long) {
        _messageSent.tryEmit(userId)
    }
}

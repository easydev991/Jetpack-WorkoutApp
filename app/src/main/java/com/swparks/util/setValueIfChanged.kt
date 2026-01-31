package com.swparks.util

import kotlinx.coroutines.flow.MutableStateFlow

inline fun <T> MutableStateFlow<T>.setValueIfChanged(newValue: T, onUpdated: () -> Unit) {
    if (this.value != newValue) {
        this.value = newValue
        onUpdated()
    }
}

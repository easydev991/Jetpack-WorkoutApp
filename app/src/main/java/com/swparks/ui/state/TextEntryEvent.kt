package com.swparks.ui.state

/**
 * Одноразовые события для экрана ввода текста.
 */
sealed class TextEntryEvent {
    data object Success : TextEntryEvent()

    data class Error(
        val message: String
    ) : TextEntryEvent()
}

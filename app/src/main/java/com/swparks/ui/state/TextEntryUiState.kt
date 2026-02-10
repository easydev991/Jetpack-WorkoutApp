package com.swparks.ui.state

import com.swparks.ui.model.TextEntryMode

/**
 * UI State для экрана ввода текста.
 */
data class TextEntryUiState(
    val mode: TextEntryMode,
    val text: String = "",
    val isLoading: Boolean = false,
    val isSendEnabled: Boolean = false,
    val error: String? = null
)

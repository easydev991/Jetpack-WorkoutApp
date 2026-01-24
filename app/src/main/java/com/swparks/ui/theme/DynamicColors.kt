package com.swparks.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Утилита для работы с динамическими цветами Material 3.
 */
object DynamicColors {
    /**
     * Проверяет, доступны ли динамические цвета на текущем устройстве.
     * Динамические цвета доступны только на Android 12+.
     *
     * @return true если Android 12+, иначе false
     */
    @Composable
    fun isDynamicColorAvailable(): Boolean =
        remember {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
}

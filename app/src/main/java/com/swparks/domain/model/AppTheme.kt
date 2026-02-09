package com.swparks.domain.model

import com.swparks.domain.model.AppTheme.DARK
import com.swparks.domain.model.AppTheme.LIGHT
import com.swparks.domain.model.AppTheme.SYSTEM


/**
 * Перечисление доступных тем приложения.
 *
 * @property LIGHT Светлая тема
 * @property DARK Тёмная тема
 * @property SYSTEM Системная тема (следует настройкам системы)
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM,
}

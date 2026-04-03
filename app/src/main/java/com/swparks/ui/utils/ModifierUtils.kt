package com.swparks.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Значение прозрачности для заблокированных (disabled) элементов
 */
const val DISABLED_ALPHA = 0.5f

/**
 * Применяет визуальную блокировку к элементу:
 * - Устанавливает alpha = 0.5f при disabled
 * - Блокирует клики при disabled
 *
 * @param disabled Условие блокировки (true = элемент заблокирован)
 * @param onClick Обработчик клика
 */
fun Modifier.disabledIf(
    disabled: Boolean,
    onClick: () -> Unit
): Modifier =
    this
        .disabledAlpha(disabled)
        .clickable(enabled = !disabled, onClick = onClick)

/**
 * Применяет только визуальную прозрачность для заблокированного элемента.
 * Используется когда clickable/selection обрабатывается отдельно.
 *
 * @param disabled Условие блокировки (true = элемент заблокирован)
 */
fun Modifier.disabledAlpha(disabled: Boolean): Modifier =
    this
        .graphicsLayer {
            alpha = if (disabled) DISABLED_ALPHA else 1f
        }

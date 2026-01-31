package com.swparks.ui.ds

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Модификатор для блокировки всех вертикальных жестов (свайп вниз/вверх) и горизонтальных жестов.
 *
 * Перехватывает все жесты и блокирует их для предотвращения случайных действий.
 * Используется для предотвращения случайного закрытия ModalBottomSheet жестами.
 *
 * @param threshold Минимальное расстояние свайпа для активации блокировки
 */
fun Modifier.disableAllGestures(
    threshold: Float = 0.5f
): Modifier = pointerInput(Unit) {
    detectDragGestures { change, dragAmount ->
        // Блокируем все жесты (как вертикальные, так и горизонтальные)
        // Порог чувствительности для игнорирования случайных касаний
        if (abs(dragAmount.x) > threshold || abs(dragAmount.y) > threshold) {
            change.consume()
        }
    }
}

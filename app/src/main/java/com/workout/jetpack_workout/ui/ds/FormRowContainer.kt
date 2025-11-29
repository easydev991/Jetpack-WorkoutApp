package com.workout.jetpack_workout.ui.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Обертка для заворачивания контента в горизонтальный стек
 *
 * @param modifier Модификатор
 * @param verticalAlignment Выравнивание по вертикали
 * @param horizontalArrangement Расположение в стеке по горизонтали
 * @param horizontalPadding Горизонтальный паддинг
 * @param verticalPadding Вертикальный паддинг
 * @param content Контент
 */
@Composable
fun FormRowContainer(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 0.dp,
    content: @Composable (RowScope.() -> Unit)
) {
    Row(
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
    ) {
        content()
    }
}
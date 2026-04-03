package com.swparks.ui.ds

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
 * Конфигурация для отображения горизонтального стека
 *
 * @param modifier Модификатор
 * @param verticalAlignment Выравнивание по вертикали
 * @param horizontalArrangement Расположение в стеке по горизонтали
 * @param horizontalPadding Горизонтальный паддинг
 * @param verticalPadding Вертикальный паддинг
 * @param content Контент
 */
data class FormRowConfig(
    val modifier: Modifier = Modifier,
    val verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    val horizontalArrangement: Arrangement.Horizontal,
    val horizontalPadding: Dp = 12.dp,
    val verticalPadding: Dp = 0.dp,
    val content: @Composable (RowScope.() -> Unit)
)

/**
 * Обертка для заворачивания контента в горизонтальный стек
 *
 * @param config Конфигурация для отображения - [FormRowConfig]
 */
@Composable
fun FormRowContainer(config: FormRowConfig) {
    Row(
        horizontalArrangement = config.horizontalArrangement,
        verticalAlignment = config.verticalAlignment,
        modifier =
            config.modifier
                .fillMaxWidth()
                .padding(
                    horizontal = config.horizontalPadding,
                    vertical = config.verticalPadding
                ),
        content = config.content
    )
}

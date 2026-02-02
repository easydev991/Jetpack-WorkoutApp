package com.swparks.ui.screen.components.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Переиспользуемый компонент Radio button для выбора опций.
 *
 * Используется в экранах выбора темы и опции отображения события.
 *
 * @param text Текст для радио-кнопки
 * @param selected Флаг, указывающий, выбрана ли эта опция
 * @param onClick Обработчик клика по радио-кнопке
 * @param onClickable Флаг, указывающий, можно ли кликнуть по радио-кнопке
 */
@Composable
fun RadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClickable: Boolean = true,
) {
    val rowModifier =
        if (onClickable) {
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
        } else {
            Modifier
        }

    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // null recommended for accessibility with screen readers
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
            textAlign = TextAlign.Start,
        )
    }
}

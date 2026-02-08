package com.swparks.ui.ds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R

/**
 * Компонент для отображения состояния пустого списка с текстом и кнопкой действия.
 *
 * @param modifier Модификатор для настройки внешнего вида
 * @param text Локализованный текст для отображения
 * @param buttonTitle Локализованный заголовок кнопки
 * @param enabled Включена ли кнопка (по умолчанию true)
 * @param onButtonClick Замыкание для обработки нажатия на кнопку
 */
@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    text: String,
    buttonTitle: String,
    enabled: Boolean = true,
    onButtonClick: () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                enabled = enabled,
                onClick = onButtonClick
            ) {
                Text(text = buttonTitle)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateViewPreview() {
    EmptyStateView(
        text = "No items here yet",
        buttonTitle = "Create item"
    )
}

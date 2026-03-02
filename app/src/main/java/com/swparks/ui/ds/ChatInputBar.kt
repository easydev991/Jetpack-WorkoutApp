package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Минимальная высота поля ввода сообщения
 */
private val MinTextFieldHeight = 42.dp

/**
 * Компонент ввода сообщения с полем ввода и кнопкой отправки.
 *
 * @param modifier Модификатор
 * @param text Текст сообщения
 * @param onTextChange Callback при изменении текста
 * @param isLoading Индикатор загрузки (отправки сообщения)
 * @param onSendClick Callback при нажатии на кнопку отправки
 */
@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean = false,
    onSendClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        SWTextField(
            config = TextFieldConfig(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinTextFieldHeight),
                text = text,
                labelID = R.string.message_placeholder,
                singleLine = false,
                enabled = !isLoading,
                onTextChange = onTextChange
            )
        )
        SendChatMessageButton(
            enabled = text.isNotEmpty() && !isLoading,
            onClick = onSendClick
        )
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun ChatInputBarPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular)),
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))
            ) {
                // Пустое поле
                ChatInputBar(
                    text = "",
                    onTextChange = {},
                    isLoading = false,
                    onSendClick = {}
                )
                // С текстом
                ChatInputBar(
                    text = "Привет! Как дела?",
                    onTextChange = {},
                    isLoading = false,
                    onSendClick = {}
                )
                // В состоянии загрузки
                ChatInputBar(
                    text = "Отправляю...",
                    onTextChange = {},
                    isLoading = true,
                    onSendClick = {}
                )
            }
        }
    }
}

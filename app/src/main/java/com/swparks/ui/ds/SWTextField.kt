package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Основной текстфилд. Лучше использовать mutableStateOf для хранения текста во вьюмодели
 *
 * @param modifier Модификатор
 * @param text Текст
 * @param labelID Идентификатор локализованной строки с плейсхолдером
 * @param secure Нужно ли скрыть текст (например, для пароля)
 * @param singleLine Нужно ли ограничить текст одной строкой
 * @param isError Состояние ошибки
 * @param enabled Доступность текстфилда для ввода текста
 * @param supportingText Текст-подсказка внизу
 * @param onTextChange Возвращает текст при вводе
 */
@Composable
fun SWTextField(
    modifier: Modifier = Modifier,
    text: String,
    @StringRes labelID: Int,
    secure: Boolean = false,
    singleLine: Boolean = true,
    isError: Boolean = false,
    enabled: Boolean = true,
    supportingText: String = "",
    onTextChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = text,
        onValueChange = onTextChange,
        singleLine = singleLine,
        label = { Text(text = stringResource(id = labelID)) },
        supportingText = {
            if (supportingText.isNotBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = supportingText,
                    textAlign = TextAlign.Start,
                )
            }
        },
        shape = RoundedCornerShape(8.dp),
        visualTransformation = if (secure)
            PasswordVisualTransformation()
        else VisualTransformation.None,
        isError = isError,
        enabled = enabled
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun SWTextFieldPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SWTextField(
                    text = "",
                    labelID = R.string.login,
                    onTextChange = {}
                )
                SWTextField(
                    text = "NineNineOne",
                    labelID = R.string.login,
                    onTextChange = {}
                )
                SWTextField(
                    text = "123123123",
                    secure = true,
                    labelID = R.string.new_password,
                    onTextChange = {}
                )
                SWTextField(
                    text = "123",
                    secure = true,
                    isError = true,
                    supportingText = stringResource(id = R.string.password_short),
                    labelID = R.string.new_password,
                    onTextChange = {}
                )
            }
        }
    }
}
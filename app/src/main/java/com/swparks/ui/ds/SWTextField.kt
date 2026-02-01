package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Конфигурация для отображения текстового поля
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
 * @param focusRequester Объект для управления фокусом текстового поля
 */
data class TextFieldConfig(
    val modifier: Modifier = Modifier,
    val text: String,
    @param:StringRes val labelID: Int,
    val secure: Boolean = false,
    val singleLine: Boolean = true,
    val isError: Boolean = false,
    val enabled: Boolean = true,
    val supportingText: String = "",
    val onTextChange: (String) -> Unit,
    val focusRequester: FocusRequester? = null,
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val keyboardActions: KeyboardActions = KeyboardActions.Default
)

/**
 * Основной текстфилд. Лучше использовать mutableStateOf для хранения текста во вьюмодели
 *
 * @param config Конфигурация для отображения - [TextFieldConfig]
 */
@Composable
fun SWTextField(config: TextFieldConfig) {
    OutlinedTextField(
        modifier = config.modifier
            .fillMaxWidth()
            .then(
                if (config.focusRequester != null) {
                    Modifier.focusRequester(config.focusRequester)
                } else {
                    Modifier
                }
            ),
        value = config.text,
        onValueChange = config.onTextChange,
        singleLine = config.singleLine,
        label = { Text(text = stringResource(id = config.labelID)) },
        supportingText = {
            if (config.supportingText.isNotBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = config.supportingText,
                    textAlign = TextAlign.Start,
                )
            }
        },
        shape = RoundedCornerShape(dimensionResource(R.dimen.spacing_xsmall)),
        visualTransformation = if (config.secure)
            PasswordVisualTransformation()
        else VisualTransformation.None,
        isError = config.isError,
        enabled = config.enabled,
        keyboardOptions = config.keyboardOptions,
        keyboardActions = config.keyboardActions
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
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular)),
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
            ) {
                SWTextField(
                    config = TextFieldConfig(
                        text = "",
                        labelID = R.string.login,
                        onTextChange = {}
                    )
                )
                SWTextField(
                    config = TextFieldConfig(
                        text = "NineNineOne",
                        labelID = R.string.login,
                        onTextChange = {}
                    )
                )
                SWTextField(
                    config = TextFieldConfig(
                        text = "123123123",
                        secure = true,
                        labelID = R.string.new_password,
                        onTextChange = {}
                    )
                )
                SWTextField(
                    config = TextFieldConfig(
                        text = "123",
                        secure = true,
                        isError = true,
                        supportingText = stringResource(id = R.string.password_short),
                        labelID = R.string.new_password,
                        onTextChange = {}
                    )
                )
            }
        }
    }
}
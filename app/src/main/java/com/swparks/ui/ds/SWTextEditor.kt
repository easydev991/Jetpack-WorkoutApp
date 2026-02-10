package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Вьюшка для ввода большого количества текста. Например, для записи в дневнике
 *
 * @param modifier Модификатор
 * @param text Основной текст
 * @param labelID Идентификатор локализованной строки с плейсхолдером (null = нет label)
 * @param enabled Доступность текстового поля для ввода текста
 * @param onTextChange Возвращает текст при вводе
 */
@Composable
fun SWTextEditor(
    modifier: Modifier = Modifier,
    text: String,
    @StringRes labelID: Int? = null,
    enabled: Boolean = true,
    onTextChange: (String) -> Unit
) {
    SWTextField(
        config = TextFieldConfig(
            modifier = modifier.defaultMinSize(minHeight = 104.dp),
            text = text,
            labelID = labelID,
            onTextChange = onTextChange,
            enabled = enabled,
            singleLine = false
        )
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun SWTextEditorPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                SWTextEditor(
                    text = "",
                    labelID = R.string.event_details_placeholder,
                    onTextChange = {}
                )
                SWTextEditor(
                    text = "Мероприятие будет длится около трех часов, так что можно приходить в любое удобное время.",
                    labelID = R.string.event_details_placeholder,
                    onTextChange = {}
                )
            }
        }
    }
}
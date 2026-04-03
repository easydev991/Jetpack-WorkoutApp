package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.utils.disabledAlpha

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
fun SWRadioButton(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClickable: Boolean = true
) {
    val rowModifier =
        if (onClickable && !selected) {
            modifier.selectable(
                selected = false,
                onClick = onClick,
                role = Role.RadioButton
            )
        } else {
            modifier
        }

    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .disabledAlpha(!onClickable),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null recommended for accessibility with screen readers
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
    }
}

@Preview(showBackground = true, locale = "ru", name = "Selected - Clickable")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru",
    name = "Selected - Clickable - Dark"
)
@Composable
private fun SWRadioButtonPreviewSelectedClickable() {
    com.swparks.ui.theme.JetpackWorkoutAppTheme {
        Surface {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))) {
                SWRadioButton(
                    text = "Option 1",
                    selected = true,
                    onClick = {},
                    onClickable = true
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru", name = "Unselected - Clickable")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru",
    name = "Unselected - Clickable - Dark"
)
@Composable
private fun SWRadioButtonPreviewUnselectedClickable() {
    com.swparks.ui.theme.JetpackWorkoutAppTheme {
        Surface {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))) {
                SWRadioButton(
                    text = "Option 1",
                    selected = false,
                    onClick = {},
                    onClickable = true
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru", name = "Selected - Disabled")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru",
    name = "Selected - Disabled - Dark"
)
@Composable
private fun SWRadioButtonPreviewSelectedDisabled() {
    com.swparks.ui.theme.JetpackWorkoutAppTheme {
        Surface {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))) {
                SWRadioButton(
                    text = "Option 1",
                    selected = true,
                    onClick = {},
                    onClickable = false
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru", name = "Unselected - Disabled")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru",
    name = "Unselected - Disabled - Dark"
)
@Composable
private fun SWRadioButtonPreviewUnselectedDisabled() {
    com.swparks.ui.theme.JetpackWorkoutAppTheme {
        Surface {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))) {
                SWRadioButton(
                    text = "Option 1",
                    selected = false,
                    onClick = {},
                    onClickable = false
                )
            }
        }
    }
}

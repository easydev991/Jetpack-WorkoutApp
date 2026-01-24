package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Размер кнопки
 *
 * @property horizontalSpacing Расстояние между текстом и иконкой
 * @property paddingValues Паддинги для контента кнопки
 * @property iconSize Размер иконки
 */
enum class SWButtonSize(
    val horizontalSpacing: Dp,
    val paddingValues: PaddingValues,
    val iconSize: Dp
) {
    /**
     * Большой размер (стандартный)
     */
    LARGE(
        horizontalSpacing = 10.dp,
        paddingValues = PaddingValues(
            horizontal = 20.dp,
            vertical = 12.dp
        ),
        iconSize = 19.dp
    ),

    /**
     * Маленький размер
     */
    SMALL(
        horizontalSpacing = 6.dp,
        paddingValues = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        iconSize = 15.dp
    )
}

/**
 * Вариант кнопки
 */
enum class SWButtonMode {
    FILLED,
    TINTED
}

/**
 * Конфигурация для отображения основной кнопки
 *
 * @param modifier Модификатор
 * @param size Размер - [SWButtonSize]
 * @param mode Вариант - [SWButtonMode]
 * @param imageVector Иконка
 * @param text Текст
 * @param enabled Доступность кнопки для нажатий
 * @param onClick Действие при нажатии
 */
data class ButtonConfig(
    val modifier: Modifier = Modifier,
    val size: SWButtonSize = SWButtonSize.LARGE,
    val mode: SWButtonMode = SWButtonMode.FILLED,
    val imageVector: ImageVector? = null,
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Основная кнопка
 *
 * @param config Конфигурация для отображения - [ButtonConfig]
 */
@Composable
fun SWButton(config: ButtonConfig) {
    Button(
        modifier = config.modifier
            .scaleOnTap()
            .let {
                if (config.size == SWButtonSize.LARGE) {
                    return@let it.fillMaxWidth()
                }
                it
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = when (config.mode) {
                SWButtonMode.FILLED -> MaterialTheme.colorScheme.primary
                SWButtonMode.TINTED -> MaterialTheme.colorScheme.secondary
            },
            contentColor = when (config.mode) {
                SWButtonMode.FILLED -> MaterialTheme.colorScheme.onPrimary
                SWButtonMode.TINTED -> MaterialTheme.colorScheme.onSecondary
            }
        ),
        contentPadding = config.size.paddingValues,
        shape = RoundedCornerShape(dimensionResource(R.dimen.spacing_small)),
        enabled = config.enabled,
        onClick = config.onClick
    ) {
        if (config.imageVector != null) {
            Image(
                modifier = Modifier
                    .padding(end = config.size.horizontalSpacing)
                    .size(config.size.iconSize),
                imageVector = config.imageVector,
                colorFilter = ColorFilter.tint(
                    if (config.mode == SWButtonMode.FILLED)
                        MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondary
                ),
                contentDescription = null
            )
        }
        Text(text = config.text)
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
fun SWButtonPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PreviewSmallButtons()
                PreviewLargeButtons()
            }
        }
    }
}

@Composable
private fun PreviewSmallButtons() {
    SWButton(
        config = ButtonConfig(
            size = SWButtonSize.SMALL,
            text = stringResource(id = R.string.authorization),
            enabled = false,
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            size = SWButtonSize.SMALL,
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            size = SWButtonSize.SMALL,
            text = stringResource(id = R.string.authorization),
            imageVector = Icons.Default.Add,
            onClick = {}
        )
    )
}

@Composable
private fun PreviewLargeButtons() {
    SWButton(
        config = ButtonConfig(
            text = stringResource(id = R.string.authorization),
            enabled = false,
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            text = stringResource(id = R.string.authorization),
            imageVector = Icons.Default.Add,
            onClick = {}
        )
    )
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Composable
fun SWButtonTintedPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PreviewTintedButtons()
            }
        }
    }
}

@Composable
private fun PreviewTintedButtons() {
    SWButton(
        config = ButtonConfig(
            size = SWButtonSize.SMALL,
            mode = SWButtonMode.TINTED,
            enabled = false,
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            size = SWButtonSize.SMALL,
            mode = SWButtonMode.TINTED,
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            size = SWButtonSize.SMALL,
            mode = SWButtonMode.TINTED,
            imageVector = Icons.Default.Add,
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Composable
fun SWButtonMixedPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PreviewMixedButtons()
            }
        }
    }
}

@Composable
private fun PreviewMixedButtons() {
    SWButton(
        config = ButtonConfig(
            mode = SWButtonMode.TINTED,
            enabled = false,
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            mode = SWButtonMode.TINTED,
            text = stringResource(id = R.string.authorization),
            onClick = {}
        )
    )
    SWButton(
        config = ButtonConfig(
            imageVector = Icons.Default.Add,
            text = stringResource(id = R.string.authorization),
            mode = SWButtonMode.TINTED,
            onClick = {}
        )
    )
}

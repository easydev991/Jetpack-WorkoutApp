package com.workout.jetpack_workout.ui.ds

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

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
    );
}

/**
 * Вариант кнопки
 */
enum class SWButtonMode {
    FILLED,
    TINTED
}

/**
 * Основная кнопка
 *
 * @param modifier Модификатор
 * @param size Размер - [SWButtonSize]
 * @param mode Вариант - [SWButtonMode]
 * @param imageVector Иконка
 * @param text Текст
 * @param enabled Доступность кнопки для нажатий
 * @param onClick Действие при нажатии
 */
@Composable
fun SWButton(
    modifier: Modifier = Modifier,
    size: SWButtonSize = SWButtonSize.LARGE,
    mode: SWButtonMode = SWButtonMode.FILLED,
    imageVector: ImageVector? = null,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .scaleOnTap()
            .let {
                if (size == SWButtonSize.LARGE) {
                    return@let it.fillMaxWidth()
                }
                it
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = when (mode) {
                SWButtonMode.FILLED -> MaterialTheme.colorScheme.primary
                SWButtonMode.TINTED -> MaterialTheme.colorScheme.secondary
            },
            contentColor = when (mode) {
                SWButtonMode.FILLED -> MaterialTheme.colorScheme.onPrimary
                SWButtonMode.TINTED -> MaterialTheme.colorScheme.onSecondary
            }
        ),
        contentPadding = size.paddingValues,
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        onClick = onClick
    ) {
        if (imageVector != null) {
            Image(
                modifier = Modifier
                    .padding(end = size.horizontalSpacing)
                    .size(size.iconSize),
                imageVector = imageVector,
                colorFilter = ColorFilter.tint(
                    if (mode == SWButtonMode.FILLED)
                        MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondary
                ),
                contentDescription = null
            )
        }
        Text(text = text)
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SWButton(
                    size = SWButtonSize.SMALL,
                    text = stringResource(id = R.string.authorization),
                    enabled = false,
                    onClick = {}
                )
                SWButton(
                    size = SWButtonSize.SMALL,
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    size = SWButtonSize.SMALL,
                    text = stringResource(id = R.string.authorization),
                    imageVector = Icons.Default.Add,
                    onClick = {}
                )
                SWButton(
                    text = stringResource(id = R.string.authorization),
                    enabled = false,
                    onClick = {}
                )
                SWButton(
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    text = stringResource(id = R.string.authorization),
                    imageVector = Icons.Default.Add,
                    onClick = {}
                )
                SWButton(
                    size = SWButtonSize.SMALL,
                    mode = SWButtonMode.TINTED,
                    enabled = false,
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    size = SWButtonSize.SMALL,
                    mode = SWButtonMode.TINTED,
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    size = SWButtonSize.SMALL,
                    mode = SWButtonMode.TINTED,
                    imageVector = Icons.Default.Add,
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    mode = SWButtonMode.TINTED,
                    enabled = false,
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    mode = SWButtonMode.TINTED,
                    text = stringResource(id = R.string.authorization),
                    onClick = {}
                )
                SWButton(
                    imageVector = Icons.Default.Add,
                    text = stringResource(id = R.string.authorization),
                    mode = SWButtonMode.TINTED,
                    onClick = {}
                )
            }
        }
    }
}
package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@Composable
private fun FormRowViewContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FormCardContainer {
        FormRowContainer(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            content()
        }
    }
}

/**
 * В фигме называется "Ячейка формы"
 *
 * @param modifier Модификатор
 * @param leadingText Текст слева
 * @param trailingText Текст справа
 * @param badgeValue Цифра в бейджике (если передать null, то без бейджика)
 * @param enabled Влияет на отображение шеврона справа
 */
@Composable
fun FormRowView(
    modifier: Modifier = Modifier,
    leadingText: String,
    trailingText: String = "",
    badgeValue: Int? = null,
    enabled: Boolean = true
) {
    FormRowViewContainer(
        modifier = modifier.padding(vertical = 12.dp)
    ) {
        Text(
            text = leadingText,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (trailingText.isNotBlank()) {
                Text(
                    text = trailingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (badgeValue != null) {
                CircleBadgeView(
                    value = badgeValue,
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 4.dp
                    )
                )
            }
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Image(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    colorFilter = ColorFilter.tint(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentDescription = "Chevron"
                )
            }
        }
    }
}

/**
 * Свич-версия "ячейки формы"
 */
@Composable
fun SwitchFormRowView(
    modifier: Modifier = Modifier,
    leadingText: String,
    isOn: Boolean = false,
    isEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    FormRowViewContainer(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = leadingText,
            maxLines = 1
        )
        Switch(
            enabled = isEnabled,
            checked = isOn,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun FormRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                FormRowView(
                    leadingText = stringResource(id = R.string.where_trains),
                    trailingText = pluralStringResource(
                        id = R.plurals.parksCount,
                        count = 26,
                        26
                    )
                )
                FormRowView(
                    leadingText = stringResource(id = R.string.friends),
                    trailingText = pluralStringResource(
                        id = R.plurals.friendsCount,
                        count = 50,
                        50
                    )
                )
                FormRowView(
                    leadingText = stringResource(id = R.string.friends),
                    trailingText = pluralStringResource(
                        id = R.plurals.friendsCount,
                        count = 50,
                        50
                    ),
                    badgeValue = 5
                )
                SwitchFormRowView(
                    leadingText = stringResource(id = R.string.train_here),
                    isOn = false,
                    onCheckedChange = {}
                )
                SwitchFormRowView(
                    leadingText = stringResource(id = R.string.participate_too),
                    isOn = true,
                    onCheckedChange = {}
                )
            }
        }
    }
}
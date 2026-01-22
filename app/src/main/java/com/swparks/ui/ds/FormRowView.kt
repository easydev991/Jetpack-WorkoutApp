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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

private object FormRowViewPreviewConstants {
    const val PARKS_COUNT = 26
    const val FRIENDS_COUNT = 50
    const val BADGE_VALUE = 5
}

@Composable
private fun FormRowViewContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FormCardContainer {
        FormRowContainer(
            config = FormRowConfig(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                content = {
                    content()
                }
            )
        )
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
        modifier = modifier.padding(vertical = dimensionResource(id = R.dimen.spacing_small))
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
                        start = dimensionResource(id = R.dimen.spacing_regular),
                        end = dimensionResource(id = R.dimen.spacing_xxsmall)
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
        modifier = modifier.padding(vertical = dimensionResource(id = R.dimen.spacing_xxsmall))
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
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_regular)),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
            ) {
                FormRowView(
                    leadingText = stringResource(id = R.string.where_trains),
                    trailingText = pluralStringResource(
                        id = R.plurals.parksCount,
                        count = FormRowViewPreviewConstants.PARKS_COUNT,
                        FormRowViewPreviewConstants.PARKS_COUNT
                    )
                )
                FormRowView(
                    leadingText = stringResource(id = R.string.friends),
                    trailingText = pluralStringResource(
                        id = R.plurals.friendsCount,
                        count = FormRowViewPreviewConstants.FRIENDS_COUNT,
                        FormRowViewPreviewConstants.FRIENDS_COUNT
                    )
                )
                FormRowView(
                    leadingText = stringResource(id = R.string.friends),
                    trailingText = pluralStringResource(
                        id = R.plurals.friendsCount,
                        count = FormRowViewPreviewConstants.FRIENDS_COUNT,
                        FormRowViewPreviewConstants.FRIENDS_COUNT
                    ),
                    badgeValue = FormRowViewPreviewConstants.BADGE_VALUE
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
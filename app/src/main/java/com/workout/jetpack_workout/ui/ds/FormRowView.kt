package com.workout.jetpack_workout.ui.ds

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
private fun FormRowViewContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.isLight()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = CardDefaults.outlinedCardBorder(isLight)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            content()
        }
    }
}

/**
 * В фигме называется "Ячейка формы"
 */
@Composable
fun FormRowView(
    modifier: Modifier = Modifier,
    leadingText: String,
    trailingText: String = "",
    badgeValue: Int? = null,
    isEnabled: Boolean = true
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
                    color = MaterialTheme.colorScheme.secondary,
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
                visible = isEnabled,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Image(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary),
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
        modifier = modifier.padding(vertical = 6.dp)
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

@Preview(showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
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
                    leadingText = "Где тренируется",
                    trailingText = "26 площадок"
                )
                FormRowView(
                    leadingText = "Друзья",
                    trailingText = "50 друзей"
                )
                FormRowView(
                    leadingText = "Друзья",
                    trailingText = "50 друзей",
                    badgeValue = 5
                )
                SwitchFormRowView(
                    leadingText = "Тренируюсь здесь",
                    isOn = false,
                    onCheckedChange = {}
                )
                SwitchFormRowView(
                    leadingText = "Пойду на мероприятие",
                    isOn = true,
                    onCheckedChange = {}
                )
            }
        }
    }
}
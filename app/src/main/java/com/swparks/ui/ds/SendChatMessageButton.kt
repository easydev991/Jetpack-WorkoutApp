package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Круглая кнопка отправки сообщения с иконкой стрелки вверх.
 *
 * @param modifier Модификатор
 * @param enabled Доступность кнопки (серый цвет когда disabled)
 * @param onClick Callback при нажатии
 */
@Composable
fun SendChatMessageButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val containerColor =
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier =
            modifier
                .size(SendChatMessageButtonDefaults.ButtonSize)
                .testTag("SendChatMessageButton"),
        shape = CircleShape,
        color = containerColor,
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(SendChatMessageButtonDefaults.IconSize)
        )
    }
}

/**
 * Размеры по умолчанию для SendChatMessageButton
 */
private object SendChatMessageButtonDefaults {
    val ButtonSize = 39.dp
    val IconSize = 20.dp
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
fun SendChatMessageButtonPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Row(
                modifier =
                    Modifier
                        .padding(dimensionResource(R.dimen.spacing_regular))
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SendChatMessageButton(
                    enabled = true,
                    onClick = {}
                )
                SendChatMessageButton(
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}

package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * В фигме называется "Элемент списка"
 *
 * @param modifier Модификатор
 * @param leadingIconID Идентификатор для иконки слева
 * @param leadingText Текст слева
 * @param trailingText Текст справа
 * @param showChevron Показывать ли шеврон
 * @param enabled Влияет на отображение шеврона
 */
@Composable
fun ListRowView(
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconID: Int? = null,
    leadingText: String,
    trailingText: String = "",
    showChevron: Boolean = false,
    enabled: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIconID != null) {
                Image(
                    painter = painterResource(leadingIconID),
                    contentDescription = "Leading icon",
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(34.dp)
                        .background(color = MaterialTheme.colorScheme.secondary)
                )
            }
            Text(
                text = leadingText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (trailingText.isNotBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = showChevron && enabled,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Image(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    contentDescription = "Chevron"
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun ListRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                ListRowView(leadingText = "Текст")
                ListRowView(
                    leadingText = "Текст",
                    showChevron = true
                )
                ListRowView(
                    leadingText = "Текст",
                    trailingText = "подпись"
                )
                ListRowView(
                    leadingText = "Текст",
                    trailingText = "подпись",
                    showChevron = true
                )
                ListRowView(
                    leadingIconID = R.drawable.outline_person,
                    leadingText = "Текст"
                )
                ListRowView(
                    leadingIconID = R.drawable.outline_person,
                    leadingText = "Текст",
                    showChevron = true
                )
                ListRowView(
                    leadingIconID = R.drawable.outline_person,
                    leadingText = "Текст",
                    trailingText = "подпись"
                )
                ListRowView(
                    leadingIconID = R.drawable.outline_person,
                    leadingText = "Текст",
                    trailingText = "подпись",
                    showChevron = true
                )
            }
        }
    }
}
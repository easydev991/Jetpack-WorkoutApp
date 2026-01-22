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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
 * Данные для отображения элемента списка
 *
 * @param modifier Модификатор
 * @param leadingIconID Идентификатор для иконки слева
 * @param leadingText Текст слева
 * @param trailingText Текст справа
 * @param showChevron Показывать ли шеврон
 * @param enabled Влияет на отображение шеврона
 */
data class ListRowData(
    val modifier: Modifier = Modifier,
    @DrawableRes val leadingIconID: Int? = null,
    val leadingText: String,
    val trailingText: String = "",
    val showChevron: Boolean = false,
    val enabled: Boolean = true
)

/**
 * В фигме называется "Элемент списка"
 *
 * @param data Данные для отображения - [ListRowData]
 */
@Composable
fun ListRowView(data: ListRowData) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = data.modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (data.leadingIconID != null) {
                Image(
                    painter = painterResource(data.leadingIconID),
                    contentDescription = "Leading icon",
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(34.dp)
                        .background(color = MaterialTheme.colorScheme.secondary)
                )
            }
            Text(
                text = data.leadingText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (data.trailingText.isNotBlank()) {
                Text(
                    text = data.trailingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = data.showChevron && data.enabled,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Image(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                PreviewListRowsWithoutIcon()
                PreviewListRowsWithIcon()
            }
        }
    }
}

@Composable
private fun PreviewListRowsWithoutIcon() {
    ListRowView(
        data = ListRowData(
            leadingText = "Текст"
        )
    )
    ListRowView(
        data = ListRowData(
            leadingText = "Текст",
            showChevron = true
        )
    )
    ListRowView(
        data = ListRowData(
            leadingText = "Текст",
            trailingText = "подпись"
        )
    )
    ListRowView(
        data = ListRowData(
            leadingText = "Текст",
            trailingText = "подпись",
            showChevron = true
        )
    )
}

@Composable
private fun PreviewListRowsWithIcon() {
    ListRowView(
        data = ListRowData(
            leadingIconID = R.drawable.outline_person,
            leadingText = "Текст"
        )
    )
    ListRowView(
        data = ListRowData(
            leadingIconID = R.drawable.outline_person,
            leadingText = "Текст",
            showChevron = true
        )
    )
    ListRowView(
        data = ListRowData(
            leadingIconID = R.drawable.outline_person,
            leadingText = "Текст",
            trailingText = "подпись"
        )
    )
    ListRowView(
        data = ListRowData(
            leadingIconID = R.drawable.outline_person,
            leadingText = "Текст",
            trailingText = "подпись",
            showChevron = true
        )
    )
}
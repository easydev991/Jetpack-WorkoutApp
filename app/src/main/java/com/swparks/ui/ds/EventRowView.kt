package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Данные для отображения вьюшки мероприятия в списке
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на превью-фото
 * @param name Название мероприятия
 * @param dateString Дата проведения мероприятия
 * @param address Адрес мероприятия (город, страна)
 * @param onClick Обработчик нажатия на элемент
 */
data class EventRowData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val name: String,
    val dateString: String,
    val address: String,
    val onClick: (() -> Unit)? = null
)

/**
 * Вьюшка для мероприятия в списке
 *
 * @param data Данные для отображения - [EventRowData]
 */
@Composable
fun EventRowView(data: EventRowData) {
    FormCardContainer(
        modifier = data.modifier,
        onClick = data.onClick
    ) {
        FormRowContainer(
            config = FormRowConfig(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalPadding = 12.dp,
                content = {
                    SWAsyncImage(
                        config = AsyncImageConfig(
                            imageStringURL = data.imageStringURL,
                            size = 74.dp,
                            showBorder = false
                        )
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = data.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AdditionalInfoRow(
                                imageID = R.drawable.round_access_time_16,
                                text = data.dateString
                            )
                            AdditionalInfoRow(
                                imageID = R.drawable.outline_assistant_navigation_16,
                                text = data.address
                            )
                        }
                    }
                }
            )
        )
    }
}

/**
 * Вьюшка с доп. информацией
 *
 * @param imageID Идентификатор для картинки в ресурсах
 * @param text Текст
 */
@Composable
private fun AdditionalInfoRow(
    @DrawableRes imageID: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(id = imageID),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
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
fun EventRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            EventRowView(
                data = EventRowData(
                    imageStringURL = null,
                    name = "Открытая воскресная тренировка #3 в 2023 году",
                    dateString = "22 янв, 12:00",
                    address = "Москва"
                )
            )
        }
    }
}
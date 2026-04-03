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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Данные для отображения вьюшки с информацией о площадке в списке
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на превью-фото площадки
 * @param name Название площадки
 * @param address Адрес площадки (может быть любой, какой пришлет сервер)
 * @param peopleTrainCount Количество людей, тренирующихся на площадке
 * @param enabled Флаг доступности нажатия на элемент
 * @param onClick Обработчик нажатия на элемент
 */
data class ParkRowData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val name: String,
    val address: String? = null,
    val peopleTrainCount: Int,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)

/**
 * Вьюшка с информацией о площадке в списке
 *
 * @param data Данные для отображения - [ParkRowData]
 */
@Composable
private fun ParkRowViewContent(data: ParkRowData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall))
    ) {
        Text(
            text = data.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall_plus))) {
            data.address?.takeIf { it.isNotBlank() }?.let {
                AdditionalInfoRow(imageID = R.drawable.outline_assistant_navigation_16, text = it)
            }
            AdditionalInfoRow(
                imageID = R.drawable.outline_account_circle_16,
                text =
                    if (data.peopleTrainCount > 0) {
                        pluralStringResource(
                            id = R.plurals.peopleTrainHere,
                            count = data.peopleTrainCount,
                            data.peopleTrainCount
                        )
                    } else {
                        stringResource(id = R.string.nobody_trains_here)
                    }
            )
        }
    }
}

@Composable
fun ParkRowView(data: ParkRowData) {
    FormCardContainer(
        params =
            FormCardContainerParams(
                modifier = data.modifier,
                enabled = data.enabled,
                onClick = data.onClick
            )
    ) {
        FormRowContainer(
            config =
                FormRowConfig(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                    verticalPadding = dimensionResource(R.dimen.spacing_small),
                    content = {
                        SWAsyncImage(
                            config =
                                AsyncImageConfig(
                                    imageStringURL = data.imageStringURL,
                                    size = 84.dp,
                                    contentScale = ContentScale.Crop,
                                    showBorder = false
                                )
                        )
                        ParkRowViewContent(data)
                    }
                )
        )
    }
}

@Composable
private fun AdditionalInfoRow(
    @DrawableRes imageID: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall_plus))
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
fun ParkRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ParkRowView(
                data =
                    ParkRowData(
                        imageStringURL = null,
                        name = "N° 3 Легендарная / Средняя",
                        address = "м. Партизанская, улица 2-я Советская",
                        peopleTrainCount = 5
                    )
            )
        }
    }
}

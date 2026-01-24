package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Данные для отображения вьюшки с диалогом в списке
 *
 * @param modifier Модификатор
 * @param imageStringURL Аватар автора сообщения
 * @param authorName Имя автора
 * @param dateString Дата отправки сообщения
 * @param bodyText Текст сообщения
 * @param unreadCount Количество непрочитанных сообщений в диалоге
 */
data class DialogRowData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val authorName: String,
    val dateString: String,
    val bodyText: String,
    val unreadCount: Int? = null
)

/**
 * Вьюшка с диалогом в списке
 *
 * @param data Данные для отображения - [DialogRowData]
 */
@Composable
fun DialogRowView(data: DialogRowData) {
    FormCardContainer(modifier = data.modifier) {
        FormRowContainer(
            config = FormRowConfig(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                verticalPadding = dimensionResource(R.dimen.spacing_small),
                content = {
                    DialogRowAvatar(
                        imageStringURL = data.imageStringURL
                    )
                    DialogRowContent(
                        authorName = data.authorName,
                        dateString = data.dateString,
                        bodyText = data.bodyText,
                        unreadCount = data.unreadCount
                    )
                }
            )
        )
    }
}

@Composable
private fun DialogRowAvatar(imageStringURL: String?) {
    SWAsyncImage(
        config = AsyncImageConfig(
            imageStringURL = imageStringURL,
            size = 42.dp,
            contentScale = ContentScale.Crop,
            shape = CircleShape,
            showBorder = true
        )
    )
}

@Composable
private fun DialogRowContent(
    authorName: String,
    dateString: String,
    bodyText: String,
    unreadCount: Int?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall_plus)),
        horizontalAlignment = Alignment.Start
    ) {
        DialogRowHeader(
            authorName = authorName,
            dateString = dateString
        )
        DialogRowBody(
            bodyText = bodyText,
            unreadCount = unreadCount
        )
    }
}

@Composable
private fun DialogRowHeader(
    authorName: String,
    dateString: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = authorName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DialogRowBody(
    bodyText: String,
    unreadCount: Int?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bodyText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight(400),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (unreadCount != null && unreadCount > 0) {
            CircleBadgeView(value = unreadCount)
        }
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
fun DialogRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                DialogRowView(
                    data = DialogRowData(
                        imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                        authorName = "angryswan732",
                        dateString = "12:30",
                        bodyText = "Встретимся с ребятами в субботу, там и обсудим тренировку"
                    )
                )
                DialogRowView(
                    data = DialogRowData(
                        imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                        authorName = "angryswan732",
                        dateString = "12:30",
                        bodyText = "Встретимся с ребятами в субботу, там и обсудим тренировку",
                        unreadCount = 9
                    )
                )
            }
        }
    }
}
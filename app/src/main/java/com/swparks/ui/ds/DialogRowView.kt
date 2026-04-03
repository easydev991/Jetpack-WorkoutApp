package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
 * @param enabled Влияет на отображение шеврона справа
 * @param onLongClick Обработчик долгого нажатия с позицией (localOffset - внутри элемента, itemPosition - на экране)
 */
data class DialogRowData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val authorName: String,
    val dateString: String,
    val bodyText: String,
    val unreadCount: Int? = null,
    val enabled: Boolean = true,
    val onLongClick: ((localOffset: Offset, itemPosition: Offset) -> Unit)? = null
)

/**
 * Вьюшка с диалогом в списке
 *
 * @param data Данные для отображения - [DialogRowData]
 * @param onClick Обработчик нажатия на диалог
 */
@Composable
fun DialogRowView(
    data: DialogRowData,
    onClick: (() -> Unit)? = null
) {
    // Позиция элемента на экране
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
            Modifier.onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInRoot()
            }
    ) {
        FormCardContainer(
            params =
                FormCardContainerParams(
                    modifier = data.modifier,
                    enabled = data.enabled,
                    onClick = onClick,
                    onLongClickWithOffset =
                        if (data.onLongClick != null) {
                            { offsetX, offsetY ->
                                val localOffset = Offset(offsetX, offsetY)
                                data.onLongClick.invoke(localOffset, itemPosition)
                            }
                        } else {
                            null
                        }
                )
        ) {
            FormRowContainer(
                config =
                    FormRowConfig(
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
                                unreadCount = data.unreadCount,
                                enabled = data.enabled
                            )
                        }
                    )
            )
        }
    }
}

@Composable
private fun DialogRowAvatar(imageStringURL: String?) {
    SWAsyncImage(
        config =
            AsyncImageConfig(
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
    unreadCount: Int?,
    enabled: Boolean
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
            unreadCount = unreadCount,
            enabled = enabled
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
    unreadCount: Int?,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.heightIn(min = dimensionResource(R.dimen.icon_size_chevron))
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
        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Image(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                colorFilter =
                    ColorFilter.tint(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                contentDescription = "Chevron"
            )
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
                    data =
                        DialogRowData(
                            imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                            authorName = "angryswan732",
                            dateString = "12:30",
                            bodyText = "Встретимся с ребятами в субботу, там и обсудим тренировку"
                        )
                )
                DialogRowView(
                    data =
                        DialogRowData(
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

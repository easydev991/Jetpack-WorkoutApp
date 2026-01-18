package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Действие для комментария к площадке/мероприятию
 *
 * @property titleID Идентификатор локализованной строки с названием действия
 * @property imageVector Иконка для действия
 */
enum class CommentAction(
    @StringRes val titleID: Int,
    val imageVector: ImageVector
) {
    EDIT(
        titleID = R.string.edit,
        imageVector = Icons.Default.Edit
    ),
    REPORT(
        titleID = R.string.report,
        imageVector = Icons.Outlined.Warning
    ),
    DELETE(
        titleID = R.string.delete,
        imageVector = Icons.Outlined.Delete
    )
}

/**
 * Вьюшка с комментарием к площадке/мероприятию
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на аватар комментатора
 * @param authorName Имя комментатора
 * @param dateString Дата отправки комментария (например, 21 мая 2023)
 * @param bodyText Текст комментария
 * @param enabled Доступность кнопки с действиями
 * @param byMainUser Является ли автором основной пользователь приложения
 * @param onClickAction Действие при нажатии на кнопку с тремя точками справа
 */
@Composable
fun CommentRowView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    authorName: String,
    dateString: String,
    bodyText: String,
    enabled: Boolean = true,
    byMainUser: Boolean = false,
    onClickAction: (CommentAction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val menuActions = if (byMainUser) {
        listOf(
            CommentAction.EDIT,
            CommentAction.DELETE
        )
    } else {
        listOf(CommentAction.REPORT)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SWAsyncImage(
                    imageStringURL = imageStringURL,
                    size = 40.dp,
                    contentScale = ContentScale.Crop,
                    shape = CircleShape
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(
                    modifier = Modifier.size(24.dp),
                    enabled = enabled,
                    onClick = { showMenu = true }
                ) {
                    Image(
                        imageVector = Icons.Default.MoreVert,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(18.dp),
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    menuActions.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(id = item.titleID),
                                    color = when (item) {
                                        CommentAction.DELETE,
                                        CommentAction.REPORT -> MaterialTheme.colorScheme.error

                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            },
                            trailingIcon = {
                                Image(
                                    imageVector = item.imageVector,
                                    colorFilter = ColorFilter.tint(
                                        when (item) {
                                            CommentAction.DELETE,
                                            CommentAction.REPORT -> MaterialTheme.colorScheme.error

                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    ),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onClickAction(item)
                            }
                        )
                    }
                }
            }
        }
        Text(
            text = bodyText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight(400)
        )
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
fun CommentRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            FormCardContainer {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    CommentRowView(
                        imageStringURL = "https://workout.su/img/avatar_default.jpg",
                        authorName = "SomeUser",
                        dateString = "21 мая 2023",
                        bodyText = "Лучшая площадка в моей жизни!",
                        onClickAction = {}
                    )
                    Divider()
                    CommentRowView(
                        imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                        authorName = "NineNineOne",
                        dateString = "21 мая 2023",
                        bodyText = "Классная площадка, часто тренируюсь здесь с друзьями",
                        byMainUser = true,
                        onClickAction = {}
                    )
                }
            }
        }
    }
}
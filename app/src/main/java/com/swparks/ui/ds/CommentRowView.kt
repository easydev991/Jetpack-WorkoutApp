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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.dimensionResource
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
    @param:StringRes val titleID: Int,
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
 * Конфигурация для меню действий комментария
 *
 * @property showMenu Показано ли меню
 * @property enabled Доступность кнопки-меню
 * @property menuActions Список действий
 * @property onMenuDismiss Обработчик закрытия меню
 * @property onMenuShow Обработчик открытия меню
 * @property onClickAction Обработчик клика на действие
 */
data class CommentActionsMenuConfig(
    val showMenu: Boolean,
    val enabled: Boolean = true,
    val menuActions: List<CommentAction>,
    val onMenuDismiss: () -> Unit,
    val onMenuShow: () -> Unit,
    val onClickAction: (CommentAction) -> Unit
)

/**
 * Данные для отображения вьюшки с комментарием к площадке/мероприятию
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
data class CommentRowData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val authorName: String,
    val dateString: String,
    val bodyText: String,
    val enabled: Boolean = true,
    val byMainUser: Boolean = false,
    val onClickAction: (CommentAction) -> Unit
)

/**
 * Вьюшка с комментарием к площадке/мероприятию
 *
 * @param data Данные для отображения - [CommentRowData]
 */
@Composable
fun CommentRowView(data: CommentRowData) {
    var showMenu by remember { mutableStateOf(false) }
    val menuActions = if (data.byMainUser) {
        listOf(
            CommentAction.EDIT,
            CommentAction.DELETE
        )
    } else {
        listOf(CommentAction.REPORT)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
        modifier = data.modifier.padding(dimensionResource(R.dimen.spacing_small))
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            CommentHeader(
                imageStringURL = data.imageStringURL,
                authorName = data.authorName,
                dateString = data.dateString
            )
            CommentActionsMenu(
                config = CommentActionsMenuConfig(
                    showMenu = showMenu,
                    enabled = data.enabled,
                    menuActions = menuActions,
                    onMenuDismiss = { showMenu = false },
                    onMenuShow = { showMenu = true },
                    onClickAction = data.onClickAction
                )
            )
        }
        Text(
            text = data.bodyText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun CommentHeader(
    imageStringURL: String?,
    authorName: String,
    dateString: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        SWAsyncImage(
            config = AsyncImageConfig(
                imageStringURL = imageStringURL,
                size = dimensionResource(R.dimen.size_small_plus),
                contentScale = ContentScale.Crop,
                shape = CircleShape
            )
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall_plus))
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
}

@Composable
private fun CommentActionsMenu(config: CommentActionsMenuConfig) {
    Box {
        IconButton(
            modifier = Modifier.size(dimensionResource(R.dimen.size_xsmall)),
            enabled = config.enabled,
            onClick = config.onMenuShow
        ) {
            Image(
                imageVector = Icons.Default.MoreVert,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(18.dp),
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = config.showMenu,
            onDismissRequest = config.onMenuDismiss
        ) {
            config.menuActions.forEach { item ->
                CommentDropdownMenuItem(
                    action = item,
                    onClick = {
                        config.onMenuDismiss()
                        config.onClickAction(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentDropdownMenuItem(
    action: CommentAction,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(id = action.titleID),
                color = getCommentActionColor(action)
            )
        },
        trailingIcon = {
            Image(
                imageVector = action.imageVector,
                colorFilter = ColorFilter.tint(getCommentActionColor(action)),
                contentDescription = null
            )
        },
        onClick = onClick
    )
}

@Composable
private fun getCommentActionColor(action: CommentAction) = when (action) {
    CommentAction.DELETE,
    CommentAction.REPORT -> MaterialTheme.colorScheme.error

    else -> MaterialTheme.colorScheme.onPrimaryContainer
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
                        data = CommentRowData(
                            imageStringURL = "https://workout.su/img/avatar_default.jpg",
                            authorName = "SomeUser",
                            dateString = "21 мая 2023",
                            bodyText = "Лучшая площадка в моей жизни!",
                            onClickAction = {}
                        )
                    )
                    HorizontalDivider()
                    CommentRowView(
                        data = CommentRowData(
                            imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                            authorName = "NineNineOne",
                            dateString = "21 мая 2023",
                            bodyText = "Классная площадка, часто тренируюсь здесь с друзьями",
                            byMainUser = true,
                            onClickAction = {}
                        )
                    )
                }
            }
        }
    }
}
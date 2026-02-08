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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Действие для дневника/записи в дневнике
 *
 * @property titleID Идентификатор локализованной строки с названием действия
 * @property imageVector Иконка для действия
 */
enum class JournalAction(
    @param:StringRes val titleID: Int,
    val imageVector: ImageVector
) {
    EDIT(
        titleID = R.string.edit,
        imageVector = Icons.Default.Edit
    ),
    SETUP(
        titleID = R.string.setup_action,
        imageVector = Icons.Outlined.Settings
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
 * Тип вьюшки
 *
 * @property messageMaxLines Лимит строк для основного текста вьюшки
 */
enum class JournalRowMode(val messageMaxLines: Int) {
    /**
     * Дневник
     */
    ROOT(messageMaxLines = 2),

    /**
     * Запись в дневнике
     */
    ENTRY(messageMaxLines = Int.MAX_VALUE)
}

/**
 * Конфигурация для меню действий дневника/записи в дневнике
 *
 * @property showMenu Показано ли меню
 * @property enabled Доступность кнопки-меню
 * @property actions Список действий
 * @property onMenuDismiss Обработчик закрытия меню
 * @property onMenuShow Обработчик открытия меню
 * @property onClickAction Обработчик клика на действие
 */
data class JournalActionsMenuConfig(
    val showMenu: Boolean,
    val enabled: Boolean = true,
    val actions: List<JournalAction>,
    val onMenuDismiss: () -> Unit,
    val onMenuShow: () -> Unit,
    val onClickAction: (JournalAction) -> Unit
)

/**
 * Данные для отображения вьюшки дневника/записи в дневнике в списке
 *
 * @param modifier Модификатор
 * @param imageStringURL Ссылка на аватар автора дневника/записи
 * @param title Заголовок для вьюшки
 * @param dateString Дата публикации
 * @param bodyText Основной текст
 * @param mode Режим вьюшки - [JournalRowMode]
 * @param enabled Доступность кнопки-меню с доп. действиями
 * @param actions Доп. действия для кнопки-меню
 * @param onClickAction Действие при нажатии на кнопку в меню
 */
data class JournalRowData(
    val modifier: Modifier = Modifier,
    val imageStringURL: String?,
    val title: String,
    val dateString: String,
    val bodyText: String,
    val mode: JournalRowMode,
    val enabled: Boolean = true,
    val actions: List<JournalAction>,
    val onClickAction: (JournalAction) -> Unit
)

/**
 * Вьюшка для дневника/записи в дневнике в списке
 *
 * @param data Данные для отображения - [JournalRowData]
 */
@Composable
fun JournalRowView(data: JournalRowData) {
    var showMenu by remember { mutableStateOf(false) }
    FormCardContainer(modifier = data.modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xsmall)),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_small))
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                JournalHeader(
                    imageStringURL = data.imageStringURL,
                    title = data.title,
                    dateString = data.dateString,
                    modifier = Modifier.weight(1f)
                )
                JournalActionsMenu(
                    config = JournalActionsMenuConfig(
                        showMenu = showMenu,
                        enabled = data.enabled,
                        actions = data.actions,
                        onMenuDismiss = { showMenu = false },
                        onMenuShow = { showMenu = true },
                        onClickAction = data.onClickAction
                    )
                )
            }
            Text(
                text = data.bodyText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight(400),
                maxLines = data.mode.messageMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun JournalHeader(
    imageStringURL: String?,
    title: String,
    dateString: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small)),
        modifier = modifier
    ) {
        SWAsyncImage(
            config = AsyncImageConfig(
                imageStringURL = imageStringURL,
                size = dimensionResource(id = R.dimen.icon_size_medium),
                contentScale = ContentScale.Crop,
                showBorder = false
            )
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall)),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun JournalActionsMenu(config: JournalActionsMenuConfig) {
    Box {
        IconButton(
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.icon_size_menu))
                .testTag("MenuButton"),
            enabled = config.enabled,
            onClick = config.onMenuShow
        ) {
            Image(
                imageVector = Icons.Default.MoreVert,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_dropdown)),
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = config.showMenu,
            onDismissRequest = config.onMenuDismiss
        ) {
            config.actions.forEach { item ->
                JournalDropdownMenuItem(
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
private fun JournalDropdownMenuItem(
    action: JournalAction,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(id = action.titleID),
                color = getActionColor(action)
            )
        },
        trailingIcon = {
            Image(
                imageVector = action.imageVector,
                colorFilter = ColorFilter.tint(getActionColor(action)),
                contentDescription = null
            )
        },
        onClick = onClick
    )
}

@Composable
private fun getActionColor(action: JournalAction) = when (action) {
    JournalAction.DELETE,
    JournalAction.REPORT -> MaterialTheme.colorScheme.error

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
fun JournalRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                JournalRowView(
                    data = JournalRowData(
                        imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                        title = "Дневник №1",
                        dateString = "17 февраля, 10:56",
                        bodyText = "Сегодня была тренировка на стадионе. Для начала небольшая " +
                                "пробежка для разминки, затем пара подходов подтягиваний турнике, " +
                                "и несколько подходов отжиманий",
                        mode = JournalRowMode.ROOT,
                        actions = listOf(
                            JournalAction.SETUP,
                            JournalAction.DELETE
                        ),
                        onClickAction = {}
                    )
                )
                JournalRowView(
                    data = JournalRowData(
                        imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                        title = "NineNineOne",
                        dateString = "20 февраля, 10:00",
                        bodyText = "Сегодня была тренировка на стадионе. Для начала небольшая " +
                                "пробежка для разминки, затем пара подходов подтягиваний турнике, " +
                                "и несколько подходов отжиманий",
                        mode = JournalRowMode.ENTRY,
                        actions = listOf(
                            JournalAction.EDIT,
                            JournalAction.DELETE
                        ),
                        onClickAction = {}
                    )
                )
                JournalRowView(
                    data = JournalRowData(
                        imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                        title = "Тестирую дневники из мобильного приложения",
                        dateString = "20 февраля, 10:00",
                        bodyText = "Сегодня была тренировка на стадионе. Для начала небольшая " +
                                "пробежка для разминки, затем пара подходов подтягиваний турнике, " +
                                "и несколько подходов отжиманий",
                        mode = JournalRowMode.ENTRY,
                        actions = listOf(JournalAction.REPORT),
                        onClickAction = {}
                    )
                )
            }
        }
    }
}
package com.workout.jetpack_workout.ui.ds

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

enum class JournalAction(
    @StringRes val titleID: Int,
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

enum class JournalRowMode(val messageMaxLines: Int) {
    ROOT(messageMaxLines = 2),
    ENTRY(messageMaxLines = Int.MAX_VALUE)
}

@Composable
fun JournalRowView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    title: String,
    dateString: String,
    bodyText: String,
    mode: JournalRowMode,
    enabled: Boolean = true,
    actions: List<JournalAction>,
    onClickAction: (JournalAction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    FormCardContainer(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    SWAsyncImage(
                        imageStringURL = imageStringURL,
                        size = 42.dp,
                        contentScale = ContentScale.Crop,
                        showBorder = false
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        actions.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(id = item.titleID),
                                        color = when (item) {
                                            JournalAction.DELETE,
                                            JournalAction.REPORT -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                },
                                trailingIcon = {
                                    Image(
                                        imageVector = item.imageVector,
                                        colorFilter = ColorFilter.tint(
                                            when (item) {
                                                JournalAction.DELETE,
                                                JournalAction.REPORT -> MaterialTheme.colorScheme.error
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
                fontWeight = FontWeight(400),
                maxLines = mode.messageMaxLines,
                overflow = TextOverflow.Ellipsis
            )
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
fun JournalRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                JournalRowView(
                    imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                    title = "Дневник №1",
                    dateString = "17 февраля, 10:56",
                    bodyText = "Сегодня была тренировка на стадионе. Для начала небольшая пробежка для разминки, затем пара подходов подтягиваний турнике, и несколько подходов отжиманий",
                    mode = JournalRowMode.ROOT,
                    actions = listOf(
                        JournalAction.SETUP,
                        JournalAction.DELETE
                    ),
                    onClickAction = {}
                )
                JournalRowView(
                    imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                    title = "NineNineOne",
                    dateString = "20 февраля, 10:00",
                    bodyText = "Сегодня была тренировка на стадионе. Для начала небольшая пробежка для разминки, затем пара подходов подтягиваний турнике, и несколько подходов отжиманий",
                    mode = JournalRowMode.ENTRY,
                    actions = listOf(
                        JournalAction.EDIT,
                        JournalAction.DELETE
                    ),
                    onClickAction = {}
                )
                JournalRowView(
                    imageStringURL = "https://workout.su/uploads/avatars/2023/01/2023-01-06-16-01-16-qyj.png",
                    title = "NineNineOne",
                    dateString = "20 февраля, 10:00",
                    bodyText = "Сегодня была тренировка на стадионе. Для начала небольшая пробежка для разминки, затем пара подходов подтягиваний турнике, и несколько подходов отжиманий",
                    mode = JournalRowMode.ENTRY,
                    actions = listOf(JournalAction.REPORT),
                    onClickAction = {}
                )
            }
        }
    }
}
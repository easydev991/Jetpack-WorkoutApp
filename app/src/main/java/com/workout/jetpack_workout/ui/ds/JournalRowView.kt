package com.workout.jetpack_workout.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
        imageVector = Icons.Default.Settings
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

@Composable
fun JournalRowView(
    modifier: Modifier = Modifier,
    imageStringURL: String?,
    title: String,
    dateString: String,
    bodyText: String,
    actions: List<JournalAction>
) {
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = { TODO(reason = "Добавить действия") }
                ) {
                    Image(
                        imageVector = Icons.Default.MoreVert,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(18.dp),
                        contentDescription = null
                    )
                }
            }
            Text(
                text = bodyText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight(400),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
fun JournalRowViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                JournalRowView(
                    imageStringURL = null,
                    title = "Дневник №1",
                    dateString = "17 февраля, 10:56",
                    bodyText = "Начала тренировку с легкой пробежки.",
                    actions = listOf(
                        JournalAction.EDIT,
                        JournalAction.DELETE
                    )
                )
                JournalRowView(
                    imageStringURL = null,
                    title = "Beautifulbutterfly101",
                    dateString = "20 февраля, 10:00",
                    bodyText = "Сегодня тренировалась на стадионе. Для начала небольшая пробежка для разминки, затем пара подход",
                    actions = listOf(
                        JournalAction.EDIT,
                        JournalAction.DELETE
                    )
                )
            }
        }
    }
}
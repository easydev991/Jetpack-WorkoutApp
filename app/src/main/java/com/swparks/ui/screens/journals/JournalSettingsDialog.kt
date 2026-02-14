package com.swparks.ui.screens.journals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IJournalSettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Диалог настроек дневника.
 *
 * Позволяет редактировать название и уровень доступа дневника.
 *
 * @param journal Дневник для редактирования
 * @param onDismiss Обработчик закрытия диалога
 * @param viewModel ViewModel для сохранения настроек (IJournalSettingsViewModel)
 * @param isSaving Флаг сохранения настроек
 */
@Composable
fun JournalSettingsDialog(
    journal: Journal,
    onDismiss: () -> Unit,
    viewModel: IJournalSettingsViewModel,
    isSaving: Boolean
) {
    // Локальное состояние диалога
    var title by remember { mutableStateOf(TextFieldValue(journal.title ?: "")) }
    var viewAccess by remember { mutableStateOf(journal.viewAccess ?: JournalAccess.ALL) }
    var commentAccess by remember { mutableStateOf(journal.commentAccess ?: JournalAccess.ALL) }
    var titleAttemptedSave by remember { mutableStateOf(false) }

    // Проверка, есть ли изменения
    val hasChanges = remember(title.text, viewAccess, commentAccess, journal) {
        title.text != journal.title ||
                viewAccess != journal.viewAccess ||
                commentAccess != journal.commentAccess
    }

    // Кнопка "Сохранить" активна только если название не пустое и есть изменения
    val isSaveButtonEnabled = title.text.isNotBlank() && hasChanges

    val configuration = LocalWindowInfo.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = configuration.containerDpSize.width - (dimensionResource(R.dimen.spacing_regular)) * 2),
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.journal_settings),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
        },
        text = {
            HorizontalDivider()
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            ) {
                // Текстовое поле для названия
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.journal_title_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.spacing_xsmall)),
                    singleLine = true,
                    isError = title.text.isBlank() && titleAttemptedSave
                )

                // Кто видит записи
                SettingsDialogSectionTitle(
                    text = stringResource(R.string.read_access)
                )
                JournalAccessGroup(
                    options = listOf(
                        JournalAccessOption(
                            access = JournalAccess.ALL,
                            textRes = R.string.everybody_access
                        ),
                        JournalAccessOption(
                            access = JournalAccess.FRIENDS,
                            textRes = R.string.friends_access
                        ),
                        JournalAccessOption(
                            access = JournalAccess.NOBODY,
                            textRes = R.string.only_me_access
                        )
                    ),
                    selected = viewAccess,
                    onSelected = { viewAccess = it }
                )

                // Кто может оставлять комментарии
                SettingsDialogSectionTitle(
                    text = stringResource(R.string.comment_access),
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.spacing_xsmall))
                )
                JournalAccessGroup(
                    options = listOf(
                        JournalAccessOption(
                            access = JournalAccess.ALL,
                            textRes = R.string.everybody_access
                        ),
                        JournalAccessOption(
                            access = JournalAccess.FRIENDS,
                            textRes = R.string.friends_access
                        ),
                        JournalAccessOption(
                            access = JournalAccess.NOBODY,
                            textRes = R.string.only_me_access
                        )
                    ),
                    selected = commentAccess,
                    onSelected = { commentAccess = it }
                )
                HorizontalDivider(Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall)))
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("saveButton"),
                onClick = {
                    if (title.text.isNotBlank()) {
                        viewModel.editJournalSettings(
                            journalId = journal.id,
                            title = title.text,
                            viewAccess = viewAccess,
                            commentAccess = commentAccess
                        )
                        // Диалог закроется по событию JournalSettingsSaved
                    } else {
                        titleAttemptedSave = true
                    }
                },
                enabled = isSaveButtonEnabled && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    )
}

/**
 * Вспомогательный тип для опции доступа
 */
private data class JournalAccessOption(
    val access: JournalAccess,
    val textRes: Int
)

/**
 * Заголовок секции диалога настроек
 */
@Composable
private fun SettingsDialogSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(
            top = dimensionResource(R.dimen.spacing_regular),
            bottom = dimensionResource(R.dimen.spacing_xsmall)
        )
    )
}

/**
 * Группа RadioButton для выбора доступа
 */
@Composable
private fun JournalAccessGroup(
    options: List<JournalAccessOption>,
    selected: JournalAccess,
    onSelected: (JournalAccess) -> Unit
) {
    Column(
        modifier = Modifier
            .selectableGroup()
            .padding(vertical = dimensionResource(R.dimen.spacing_xsmall)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        options.forEach { option ->
            JournalAccessRow(
                text = stringResource(option.textRes),
                selected = selected == option.access,
                onClick = { onSelected(option.access) }
            )
        }
    }
}

/**
 * Строка с RadioButton
 */
@Composable
private fun JournalAccessRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(text)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun JournalSettingsDialogPreviewLight() {
    val mockViewModel = object : IJournalSettingsViewModel {
        override val isSavingSettings: StateFlow<Boolean> = MutableStateFlow(false)

        override fun editJournalSettings(
            journalId: Long,
            title: String,
            viewAccess: JournalAccess,
            commentAccess: JournalAccess
        ) = Unit
    }

    JetpackWorkoutAppTheme {
        JournalSettingsDialog(
            journal = Journal(
                id = 1L,
                title = "Мой дневник тренировок",
                lastMessageImage = null,
                createDate = "2024-01-15T10:00:00Z",
                modifyDate = "2024-01-20T14:30:00Z",
                lastMessageDate = null,
                lastMessageText = null,
                entriesCount = null,
                ownerId = 123L,
                viewAccess = JournalAccess.FRIENDS,
                commentAccess = JournalAccess.ALL
            ),
            onDismiss = {},
            viewModel = mockViewModel,
            isSaving = false
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JournalSettingsDialogPreviewDark() {
    val mockViewModel = object : IJournalSettingsViewModel {
        override val isSavingSettings: StateFlow<Boolean> = MutableStateFlow(false)

        override fun editJournalSettings(
            journalId: Long,
            title: String,
            viewAccess: JournalAccess,
            commentAccess: JournalAccess
        ) = Unit
    }

    JetpackWorkoutAppTheme {
        JournalSettingsDialog(
            journal = Journal(
                id = 1L,
                title = "Мой дневник тренировок",
                lastMessageImage = null,
                createDate = "2024-01-15T10:00:00Z",
                modifyDate = "2024-01-20T14:30:00Z",
                lastMessageDate = null,
                lastMessageText = null,
                entriesCount = null,
                ownerId = 123L,
                viewAccess = JournalAccess.FRIENDS,
                commentAccess = JournalAccess.ALL
            ),
            onDismiss = {},
            viewModel = mockViewModel,
            isSaving = false
        )
    }
}


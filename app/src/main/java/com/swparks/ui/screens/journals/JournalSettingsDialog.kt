@file:Suppress("UnusedPrivateMember")

package com.swparks.ui.screens.journals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWRadioButton
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
    val state = rememberJournalSettingsDialogState(journal)
    val configuration = LocalWindowInfo.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = configuration.containerDpSize.width - (dimensionResource(R.dimen.spacing_regular)) * 2),
        onDismissRequest = onDismiss,
        title = { DialogTitle(onDismiss) },
        text = { DialogContent(state = state, isSaving = isSaving) },
        confirmButton = {
            SaveButton(
                isEnabled = state.isSaveButtonEnabled && !isSaving,
                onSave = {
                    if (state.title.text.isNotBlank()) {
                        viewModel.editJournalSettings(
                            journalId = journal.id,
                            title = state.title.text,
                            viewAccess = state.viewAccess,
                            commentAccess = state.commentAccess
                        )
                    } else {
                        state.titleAttemptedSave = true
                    }
                }
            )
        }
    )
}

private class JournalSettingsDialogState(
    val journal: Journal,
    title: TextFieldValue,
    viewAccess: JournalAccess,
    commentAccess: JournalAccess
) {
    var title by mutableStateOf(title)
    var viewAccess by mutableStateOf(viewAccess)
    var commentAccess by mutableStateOf(commentAccess)
    var titleAttemptedSave by mutableStateOf(false)

    val hasChanges: Boolean
        get() = title.text != journal.title ||
            viewAccess != journal.viewAccess ||
            commentAccess != journal.commentAccess

    val isSaveButtonEnabled: Boolean
        get() = title.text.isNotBlank() && hasChanges
}

@Composable
private fun rememberJournalSettingsDialogState(journal: Journal): JournalSettingsDialogState {
    return remember(journal) {
        JournalSettingsDialogState(
            journal = journal,
            title = TextFieldValue(journal.title ?: ""),
            viewAccess = journal.viewAccess ?: JournalAccess.ALL,
            commentAccess = journal.commentAccess ?: JournalAccess.ALL
        )
    }
}

@Composable
private fun DialogTitle(onDismiss: () -> Unit) {
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
}

@Composable
private fun DialogContent(
    state: JournalSettingsDialogState,
    isSaving: Boolean
) {
    Box {
        Column {
            HorizontalDivider()
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            ) {
                TitleField(state)
                ViewAccessSection(state)
                CommentAccessSection(state)
                HorizontalDivider(Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall)))
            }
        }

        if (isSaving) {
            LoadingOverlayView()
        }
    }
}

@Composable
private fun TitleField(state: JournalSettingsDialogState) {
    OutlinedTextField(
        value = state.title,
        onValueChange = { state.title = it },
        label = { Text(stringResource(R.string.journal_title_placeholder)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacing_xsmall)),
        singleLine = true,
        isError = state.title.text.isBlank() && state.titleAttemptedSave
    )
}

@Composable
private fun ViewAccessSection(state: JournalSettingsDialogState) {
    SettingsDialogSectionTitle(text = stringResource(R.string.read_access))
    JournalAccessGroup(
        options = AccessOptions,
        selected = state.viewAccess,
        onSelected = { state.viewAccess = it }
    )
}

@Composable
private fun CommentAccessSection(state: JournalSettingsDialogState) {
    SettingsDialogSectionTitle(
        text = stringResource(R.string.comment_access),
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall))
    )
    JournalAccessGroup(
        options = AccessOptions,
        selected = state.commentAccess,
        onSelected = { state.commentAccess = it }
    )
}

@Composable
private fun SaveButton(
    isEnabled: Boolean,
    onSave: () -> Unit
) {
    SWButton(
        config = ButtonConfig(
            modifier = Modifier.testTag("saveButton"),
            size = SWButtonSize.SMALL,
            text = stringResource(R.string.save),
            enabled = isEnabled,
            onClick = onSave
        )
    )
}

private val AccessOptions = listOf(
    JournalAccessOption(JournalAccess.ALL, R.string.everybody_access),
    JournalAccessOption(JournalAccess.FRIENDS, R.string.friends_access),
    JournalAccessOption(JournalAccess.NOBODY, R.string.only_me_access)
)

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
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.spacing_regular))
    )
}

/**
 * Группа SWRadioButton для выбора доступа
 */
@Composable
private fun JournalAccessGroup(
    options: List<JournalAccessOption>,
    selected: JournalAccess,
    onSelected: (JournalAccess) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        options.forEach { option ->
            SWRadioButton(
                text = stringResource(option.textRes),
                selected = selected == option.access,
                onClick = { onSelected(option.access) }
            )
        }
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

@file:Suppress("UnusedPrivateMember")

package com.swparks.ui.screens.journals

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.domain.model.JournalEntry
import com.swparks.navigation.AppState
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.JournalAction
import com.swparks.ui.ds.JournalRowData
import com.swparks.ui.ds.JournalRowMode
import com.swparks.ui.ds.JournalRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.model.EditInfo
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IJournalEntriesViewModel
import com.swparks.ui.viewmodel.JournalEntriesEvent
import com.swparks.util.DateFormatter

data class EntryPermissions(
    val canEdit: (JournalEntry) -> Boolean = { false },
    val canDelete: (JournalEntry) -> Boolean = { false },
    val canReport: Boolean = false
)

sealed class EntryAction {
    data class Delete(
        val entryId: Long
    ) : EntryAction()

    data class Edit(
        val entry: JournalEntry
    ) : EntryAction()

    data class Report(
        val entry: JournalEntry
    ) : EntryAction()
}

data class EntriesContentState(
    val entries: List<JournalEntry>,
    val isRefreshing: Boolean,
    val isDeleting: Boolean,
    val canCreateEntry: Boolean,
    val firstEntryId: Long?
)

sealed class EntriesAction {
    object Refresh : EntriesAction()

    data class Entry(
        val action: EntryAction
    ) : EntriesAction()

    object AddEntry : EntriesAction()
}

@Composable
private fun rememberEntryActionsHandler(
    context: Context,
    journalOwnerId: Long,
    journalId: Long,
    onShowDeleteDialog: (Long) -> Unit,
    onShowEditSheet: (TextEntryMode) -> Unit
): (EntriesAction) -> Unit =
    remember(context, journalOwnerId, journalId, onShowDeleteDialog, onShowEditSheet) {
        { action ->
            when (action) {
                is EntriesAction.Refresh -> {}
                is EntriesAction.Entry -> {
                    when (val entryAction = action.action) {
                        is EntryAction.Delete -> onShowDeleteDialog(entryAction.entryId)
                        is EntryAction.Edit ->
                            onShowEditSheet(
                                TextEntryMode.EditJournalEntry(
                                    ownerId = journalOwnerId,
                                    editInfo =
                                        EditInfo(
                                            parentObjectId = journalId,
                                            entryId = entryAction.entry.id,
                                            oldEntry = entryAction.entry.message ?: ""
                                        )
                                )
                            )

                        is EntryAction.Report -> {
                            val complaint =
                                com.swparks.util.Complaint.JournalEntry(
                                    author = entryAction.entry.authorName ?: "неизвестен",
                                    entryText = entryAction.entry.message ?: ""
                                )
                            com.swparks.ui.screens.more
                                .sendComplaint(complaint, context)
                        }
                    }
                }

                is EntriesAction.AddEntry -> {}
            }
        }
    }

data class JournalParams(
    val journalId: Long,
    val journalTitle: String,
    val journalOwnerId: Long,
    val journalViewAccess: String?,
    val journalCommentAccess: String?
)

sealed class ScaffoldAction {
    object Back : ScaffoldAction()

    object Settings : ScaffoldAction()

    object FabClick : ScaffoldAction()

    object Retry : ScaffoldAction()

    object Refresh : ScaffoldAction()

    data class Entry(
        val action: EntriesAction
    ) : ScaffoldAction()

    object AddEntry : ScaffoldAction()
}

private data class ScaffoldParams(
    val uiState: JournalEntriesUiState,
    val params: JournalParams,
    val appState: AppState,
    val isRefreshing: Boolean,
    val isDeleting: Boolean,
    val viewModel: IJournalEntriesViewModel
)

private data class ScaffoldState(
    val title: String,
    val isOwner: Boolean,
    val isRefreshing: Boolean,
    val isDeleting: Boolean,
    val uiState: JournalEntriesUiState,
    val canEditEntry: (JournalEntry) -> Boolean,
    val canDeleteEntry: (JournalEntry) -> Boolean
)

@Composable
private fun JournalSettingsDialogSection(
    show: Boolean,
    currentJournal: Journal?,
    params: JournalParams,
    viewModel: IJournalEntriesViewModel,
    onDismiss: () -> Unit
) {
    if (show) {
        val isSavingSettings by viewModel.isSavingSettings.collectAsState()
        val journalForDialog =
            currentJournal ?: Journal(
                id = params.journalId,
                title = params.journalTitle,
                ownerId = params.journalOwnerId,
                viewAccess =
                    params.journalViewAccess?.let { JournalAccess.valueOf(it) }
                        ?: JournalAccess.NOBODY,
                commentAccess =
                    params.journalCommentAccess?.let { JournalAccess.valueOf(it) }
                        ?: JournalAccess.NOBODY,
                lastMessageImage = null,
                createDate = null,
                modifyDate = null,
                lastMessageDate = null,
                lastMessageText = null,
                entriesCount = null
            )
        JournalSettingsDialog(
            journal = journalForDialog,
            onDismiss = onDismiss,
            viewModel = viewModel,
            isSaving = isSavingSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEntriesScaffold(
    modifier: Modifier,
    parentPaddingValues: PaddingValues,
    state: ScaffoldState,
    onAction: (ScaffoldAction) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val contentState = state.uiState as? JournalEntriesUiState.Content
    val showFab = contentState?.canCreateEntry == true && !state.isDeleting && !state.isRefreshing

    Scaffold(
        modifier = modifier.padding(bottom = parentPaddingValues.calculateBottomPadding()),
        topBar = {
            JournalEntriesTopBar(title = state.title, isOwner = state.isOwner, onAction = onAction)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (showFab) AddEntryFab(onClick = { onAction(ScaffoldAction.FabClick) })
        }
    ) { innerPadding ->
        ScaffoldContent(
            state = state,
            layoutDirection = layoutDirection,
            parentPaddingValues = parentPaddingValues,
            innerPadding = innerPadding,
            onAction = onAction
        )
    }
}

@Composable
private fun ScaffoldContent(
    state: ScaffoldState,
    layoutDirection: LayoutDirection,
    parentPaddingValues: PaddingValues,
    innerPadding: PaddingValues,
    onAction: (ScaffoldAction) -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    start = parentPaddingValues.calculateStartPadding(layoutDirection),
                    top = parentPaddingValues.calculateTopPadding(),
                    end = parentPaddingValues.calculateEndPadding(layoutDirection),
                    bottom = 0.dp
                )
                .padding(innerPadding)
    ) {
        when (state.uiState) {
            is JournalEntriesUiState.InitialLoading -> LoadingOverlayView()
            is JournalEntriesUiState.Error ->
                ErrorContentView(
                    retryAction = { onAction(ScaffoldAction.Retry) },
                    message = state.uiState.message
                )

            is JournalEntriesUiState.Content -> {
                ContentScreen(
                    state =
                        EntriesContentState(
                            state.uiState.entries,
                            state.isRefreshing,
                            state.isDeleting,
                            state.uiState.canCreateEntry,
                            state.uiState.firstEntryId
                        ),
                    permissions =
                        EntryPermissions(
                            state.canEditEntry,
                            state.canDeleteEntry,
                            false
                        ),
                    onAction = { action ->
                        when (action) {
                            is EntriesAction.Refresh -> onAction(ScaffoldAction.Refresh)
                            is EntriesAction.Entry -> onAction(ScaffoldAction.Entry(action))
                            is EntriesAction.AddEntry -> {
                                if (state.uiState.canCreateEntry) onAction(ScaffoldAction.AddEntry)
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEntriesTopBar(
    title: String,
    isOwner: Boolean,
    onAction: (ScaffoldAction) -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = { onAction(ScaffoldAction.Back) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            if (isOwner) {
                IconButton(onClick = { onAction(ScaffoldAction.Settings) }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }
    )
}

@Composable
private fun AddEntryFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.testTag("AddEntryFAB")
    ) {
        Icon(
            painterResource(R.drawable.ic_add_entry),
            stringResource(R.string.fab_add_entry_description)
        )
    }
}

/**
 * Экран списка записей в дневнике пользователя
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntriesScreen(
    modifier: Modifier = Modifier,
    params: JournalParams,
    viewModel: IJournalEntriesViewModel,
    appState: AppState,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues,
    textEntrySheetHostContent: @Composable (Boolean, TextEntryMode?, () -> Unit, () -> Unit) -> Unit =
        { show, mode, onDismissed, onSendSuccess ->
            if (show && mode != null) {
                TextEntrySheetHost(
                    show = true,
                    mode = mode,
                    onDismissed = onDismissed,
                    onSendSuccess = onSendSuccess
                )
            }
        }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Long?>(null) }
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is JournalEntriesEvent.JournalSettingsSaved) showSettingsDialog = false
        }
    }
    val entryActionHandler =
        rememberEntryActionsHandler(
            context = context,
            journalOwnerId = params.journalOwnerId,
            journalId = params.journalId,
            onShowDeleteDialog = { entryId ->
                showDeleteDialog = true
                entryToDelete = entryId
            },
            onShowEditSheet = { mode ->
                textEntryMode = mode
                showTextEntrySheet = true
            }
        )
    val scaffoldState =
        buildScaffoldState(
            params =
                ScaffoldParams(
                    uiState = uiState,
                    params = params,
                    appState = appState,
                    isRefreshing = isRefreshing,
                    isDeleting = isDeleting,
                    viewModel = viewModel
                )
        )
    JournalEntriesScaffold(
        modifier = modifier,
        parentPaddingValues = parentPaddingValues,
        state = scaffoldState,
        onAction = { action ->
            when (action) {
                is ScaffoldAction.Back -> onBackClick()
                is ScaffoldAction.Settings -> showSettingsDialog = true
                is ScaffoldAction.FabClick, is ScaffoldAction.AddEntry -> {
                    textEntryMode =
                        TextEntryMode.NewForJournal(params.journalOwnerId, params.journalId)
                    showTextEntrySheet = true
                }

                is ScaffoldAction.Retry -> viewModel.retry()
                is ScaffoldAction.Refresh -> viewModel.loadEntries()
                is ScaffoldAction.Entry -> entryActionHandler(action.action)
            }
        }
    )
    if (showDeleteDialog) {
        DeleteConfirmationDialog({ showDeleteDialog = false }) {
            entryToDelete?.let { viewModel.deleteEntry(it) }
            showDeleteDialog = false
        }
    }
    textEntrySheetHostContent(showTextEntrySheet, textEntryMode, { showTextEntrySheet = false }) {
        showTextEntrySheet = false
        if (textEntryMode is TextEntryMode.NewForJournal) viewModel.loadEntries()
    }
    JournalSettingsDialogSection(
        show = showSettingsDialog,
        currentJournal = (scaffoldState.uiState as? JournalEntriesUiState.Content)?.journal,
        params = params,
        viewModel = viewModel,
        onDismiss = { showSettingsDialog = false }
    )
}

private fun buildScaffoldState(params: ScaffoldParams): ScaffoldState {
    val isOwner = params.appState.currentUser?.id == params.params.journalOwnerId
    val currentJournal = (params.uiState as? JournalEntriesUiState.Content)?.journal
    return ScaffoldState(
        title = currentJournal?.title ?: params.params.journalTitle,
        isOwner = isOwner,
        isRefreshing = params.isRefreshing,
        isDeleting = params.isDeleting,
        uiState = params.uiState,
        canEditEntry = { params.viewModel.canEditEntry(it) },
        canDeleteEntry = { params.viewModel.canDeleteEntry(it) }
    )
}

/**
 * Контент с Pull-to-Refresh и списком записей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(
    state: EntriesContentState,
    permissions: EntryPermissions = EntryPermissions(),
    onAction: (EntriesAction) -> Unit = {}
) {
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(EntriesAction.Refresh) },
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = state.isRefreshing,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = dimensionResource(R.dimen.spacing_regular))
            )
        }
    ) {
        if (state.entries.isEmpty()) {
            if (!state.isRefreshing && state.canCreateEntry) {
                EmptyStateView(
                    text = stringResource(R.string.entries_empty),
                    buttonTitle = stringResource(R.string.create_entry),
                    enabled = !state.isDeleting,
                    onButtonClick = { onAction(EntriesAction.AddEntry) }
                )
            }
        } else {
            EntriesList(
                entries = state.entries,
                enabled = !state.isRefreshing && !state.isDeleting,
                permissions = permissions,
                firstEntryId = state.firstEntryId,
                onAction = { onAction(EntriesAction.Entry(it)) }
            )
        }

        if (state.isDeleting) {
            LoadingOverlayView()
        }
    }
}

/**
 * Список записей в дневнике
 */
@Composable
private fun EntriesList(
    entries: List<JournalEntry>,
    enabled: Boolean = true,
    permissions: EntryPermissions = EntryPermissions(),
    firstEntryId: Long? = null,
    onAction: (EntryAction) -> Unit = {}
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding =
            PaddingValues(
                start = dimensionResource(R.dimen.spacing_regular),
                top = dimensionResource(R.dimen.spacing_small),
                end = dimensionResource(R.dimen.spacing_regular),
                bottom = dimensionResource(R.dimen.spacing_regular)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            val actions = mutableListOf<JournalAction>()
            if (permissions.canEdit(entry)) {
                actions.add(JournalAction.EDIT)
            }
            if (permissions.canDelete(entry) && entry.id != firstEntryId) {
                actions.add(JournalAction.DELETE)
            }
            if (permissions.canReport) {
                actions.add(JournalAction.REPORT)
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("JournalEntry_${entry.id}")
            ) {
                JournalRowView(
                    data =
                        JournalRowData(
                            modifier = Modifier.fillMaxWidth(),
                            imageStringURL = entry.authorImage,
                            title = entry.authorName ?: "",
                            dateString =
                                DateFormatter.formatDate(
                                    context = context,
                                    dateString = entry.createDate
                                ),
                            bodyText = entry.message ?: "",
                            mode = JournalRowMode.ENTRY,
                            enabled = enabled,
                            actions = actions,
                            onClickAction = { action ->
                                when (action) {
                                    JournalAction.EDIT -> onAction(EntryAction.Edit(entry))
                                    JournalAction.DELETE -> onAction(EntryAction.Delete(entry.id))
                                    JournalAction.REPORT -> onAction(EntryAction.Report(entry))
                                    else -> {}
                                }
                            }
                        )
                )
            }
        }
    }
}

/**
 * Диалог подтверждения удаления записи
 */
@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.delete_entry_title))
        },
        text = {
            Text(stringResource(R.string.delete_entry_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
    )
}

// ==================== PREVIEWS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, locale = "ru", name = "Empty Journal Entries")
@Composable
private fun JournalEntriesScreenEmptyPreview() {
    JetpackWorkoutAppTheme {
        ContentScreen(
            state =
                EntriesContentState(
                    entries = emptyList(),
                    isRefreshing = false,
                    isDeleting = false,
                    canCreateEntry = true,
                    firstEntryId = null
                ),
            onAction = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, locale = "ru", name = "Journal Entries with Items")
@Composable
private fun JournalEntriesScreenWithItemsPreview() {
    val sampleEntries =
        listOf(
            JournalEntry(
                id = 1,
                journalId = 1,
                message = "Сегодня была отличная тренировка! Пробежал 5 км.",
                createDate = "2024-01-15T10:30:00",
                modifyDate = "2024-01-15T10:30:00",
                authorId = 1,
                authorName = "Иван",
                authorImage = null
            ),
            JournalEntry(
                id = 2,
                journalId = 1,
                message = "Настроение отличное, погода хорошая.",
                createDate = "2024-01-14T08:00:00",
                modifyDate = "2024-01-14T08:00:00",
                authorId = 1,
                authorName = "Иван",
                authorImage = null
            ),
            JournalEntry(
                id = 3,
                journalId = 1,
                message = "Завтра планирую тренировку в зале.",
                createDate = "2024-01-13T18:45:00",
                modifyDate = "2024-01-13T18:45:00",
                authorId = 1,
                authorName = "Иван",
                authorImage = null
            )
        )

    JetpackWorkoutAppTheme {
        ContentScreen(
            state =
                EntriesContentState(
                    entries = sampleEntries,
                    isRefreshing = false,
                    isDeleting = false,
                    canCreateEntry = true,
                    firstEntryId = 1
                ),
            onAction = {}
        )
    }
}

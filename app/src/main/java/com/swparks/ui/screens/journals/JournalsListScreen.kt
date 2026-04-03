@file:Suppress("UnusedPrivateMember")

package com.swparks.ui.screens.journals

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.navigation.AppState
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.JournalAction
import com.swparks.ui.ds.JournalRowData
import com.swparks.ui.ds.JournalRowMode
import com.swparks.ui.ds.JournalRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.state.JournalsUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IJournalsViewModel
import com.swparks.ui.viewmodel.JournalsEvent
import com.swparks.util.DateFormatter

/**
 * Параметры навигации для перехода к записям дневника
 */
data class JournalNavigationParams(
    val journalId: Long,
    val journalOwnerId: Long,
    val journalTitle: String,
    val viewAccess: String,
    val commentAccess: String
)

sealed class JournalsListAction {
    object Back : JournalsListAction()

    data class JournalClick(
        val params: JournalNavigationParams
    ) : JournalsListAction()
}

data class JournalsScreenParams(
    val userId: Long,
    val source: String = "profile"
)

data class JournalsScreenConfig(
    val appState: AppState,
    val params: JournalsScreenParams,
    val parentPaddingValues: PaddingValues
)

/**
 * Конфигурация отображения для владельца дневников
 */
data class OwnerDisplayConfig(
    val isOwner: Boolean,
    val onDeleteClick: (Journal) -> Unit,
    val onSetupClick: (Journal) -> Unit,
    val onCreateJournalClick: () -> Unit
)

/**
 * Состояние контента списка дневников
 */
data class JournalsContentState(
    val journals: List<Journal>,
    val isRefreshing: Boolean,
    val isDeleting: Boolean,
    val ownerConfig: OwnerDisplayConfig
)

/**
 * Actions для контента списка дневников
 */
sealed class JournalsContentAction {
    object Refresh : JournalsContentAction()

    data class JournalClick(
        val params: JournalNavigationParams
    ) : JournalsContentAction()
}

/**
 * Состояние диалогов экрана списка дневников
 */
data class JournalsDialogsState(
    val showDeleteDialog: Boolean,
    val journalToDelete: Journal?,
    val journalToEditSettings: Journal?,
    val showTextEntrySheet: Boolean,
    val textEntryMode: TextEntryMode?
)

/**
 * Параметры для JournalsMainContent
 */
data class JournalsMainContentParams(
    val uiState: JournalsUiState,
    val isRefreshing: Boolean,
    val isDeleting: Boolean,
    val ownerConfig: OwnerDisplayConfig
)

/**
 * Actions для JournalsMainContent
 */
sealed class JournalsMainContentAction {
    object Retry : JournalsMainContentAction()

    object Refresh : JournalsMainContentAction()

    data class JournalClick(
        val params: JournalNavigationParams
    ) : JournalsMainContentAction()
}

/**
 * Параметры для JournalsDialogs
 */
data class JournalsDialogsParams(
    val state: JournalsDialogsState,
    val uiState: JournalsUiState,
    val viewModel: IJournalsViewModel
)

/**
 * Actions для JournalsDialogs
 */
data class JournalsDialogsActions(
    val onDeleteDismiss: () -> Unit,
    val onDeleteConfirm: () -> Unit,
    val onSettingsDismiss: () -> Unit,
    val onTextEntryDismiss: () -> Unit,
    val onTextEntrySuccess: () -> Unit
)

/**
 * Параметры для JournalsScaffoldContent
 */
data class JournalsScaffoldContentParams(
    val innerPadding: PaddingValues,
    val parentPaddingValues: PaddingValues,
    val layoutDirection: LayoutDirection,
    val contentParams: JournalsMainContentParams,
    val dialogsParams: JournalsDialogsParams,
    val dialogsActions: JournalsDialogsActions
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalsListScreen(
    modifier: Modifier = Modifier,
    viewModel: IJournalsViewModel,
    config: JournalsScreenConfig,
    onAction: (JournalsListAction) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val isOwner = config.appState.currentUser?.id == config.params.userId

    val dialogState = rememberJournalsDialogState()
    val ownerConfig = rememberOwnerConfig(isOwner, config.params.userId, dialogState)

    JournalsEventHandler(
        viewModel = viewModel,
        getJournalToEditSettings = { dialogState.journalToEditSettings },
        onJournalSettingsSaved = { dialogState.journalToEditSettings = null }
    )

    Scaffold(
        modifier = modifier.padding(bottom = config.parentPaddingValues.calculateBottomPadding()),
        topBar = { JournalsTopBar(onBackClick = { onAction(JournalsListAction.Back) }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            JournalsFab(
                uiState = uiState,
                isDeleting = isDeleting,
                isOwner = isOwner,
                onCreateClick = ownerConfig.onCreateJournalClick
            )
        }
    ) { innerPadding ->
        JournalsScaffoldContent(
            params =
                JournalsScaffoldContentParams(
                    innerPadding = innerPadding,
                    parentPaddingValues = config.parentPaddingValues,
                    layoutDirection = layoutDirection,
                    contentParams =
                        JournalsMainContentParams(
                            uiState,
                            isRefreshing,
                            isDeleting,
                            ownerConfig
                        ),
                    dialogsParams =
                        JournalsDialogsParams(
                            dialogState.toState(),
                            uiState,
                            viewModel
                        ),
                    dialogsActions =
                        JournalsDialogsActions(
                            onDeleteDismiss = { dialogState.showDeleteDialog = false },
                            onDeleteConfirm = {
                                dialogState.journalToDelete?.let { viewModel.deleteJournal(it.id) }
                                dialogState.showDeleteDialog = false
                            },
                            onSettingsDismiss = { dialogState.journalToEditSettings = null },
                            onTextEntryDismiss = { dialogState.clearTextEntry() },
                            onTextEntrySuccess = {
                                dialogState.clearTextEntry()
                                viewModel.loadJournals()
                            }
                        )
                ),
            onContentAction = { action ->
                when (action) {
                    is JournalsMainContentAction.Retry -> viewModel.retry()
                    is JournalsMainContentAction.Refresh -> viewModel.loadJournals()
                    is JournalsMainContentAction.JournalClick ->
                        onAction(
                            JournalsListAction.JournalClick(
                                action.params
                            )
                        )
                }
            }
        )
    }
}

private class JournalsDialogState {
    var showDeleteDialog by mutableStateOf(false)
    var journalToDelete by mutableStateOf<Journal?>(null)
    var journalToEditSettings by mutableStateOf<Journal?>(null)
    var showTextEntrySheet by mutableStateOf(false)
    var textEntryMode by mutableStateOf<TextEntryMode?>(null)

    fun toState() =
        JournalsDialogsState(
            showDeleteDialog,
            journalToDelete,
            journalToEditSettings,
            showTextEntrySheet,
            textEntryMode
        )

    fun clearTextEntry() {
        showTextEntrySheet = false
        textEntryMode = null
    }
}

@Composable
private fun rememberJournalsDialogState(): JournalsDialogState = remember { JournalsDialogState() }

@Composable
private fun rememberOwnerConfig(
    isOwner: Boolean,
    userId: Long,
    dialogState: JournalsDialogState
): OwnerDisplayConfig =
    remember(isOwner, userId) {
        OwnerDisplayConfig(
            isOwner = isOwner,
            onDeleteClick = { journal ->
                dialogState.journalToDelete = journal
                dialogState.showDeleteDialog = true
            },
            onSetupClick = { journal -> dialogState.journalToEditSettings = journal },
            onCreateJournalClick = {
                dialogState.textEntryMode = TextEntryMode.NewJournal(userId)
                dialogState.showTextEntrySheet = true
            }
        )
    }

@Composable
private fun JournalsScaffoldContent(
    params: JournalsScaffoldContentParams,
    onContentAction: (JournalsMainContentAction) -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    start = params.parentPaddingValues.calculateStartPadding(params.layoutDirection),
                    top = params.parentPaddingValues.calculateTopPadding(),
                    end = params.parentPaddingValues.calculateEndPadding(params.layoutDirection)
                ).padding(params.innerPadding)
    ) {
        JournalsMainContent(params = params.contentParams, onAction = onContentAction)
    }
    JournalsDialogs(params = params.dialogsParams, actions = params.dialogsActions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalsTopBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.journals_list_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

@Composable
private fun JournalsFab(
    uiState: JournalsUiState,
    isDeleting: Boolean,
    isOwner: Boolean,
    onCreateClick: () -> Unit
) {
    if (uiState is JournalsUiState.Content && !isDeleting && isOwner) {
        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier.testTag("CreateJournalFAB")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_entry),
                contentDescription = stringResource(R.string.fab_create_journal_description)
            )
        }
    }
}

@Composable
private fun JournalsMainContent(
    params: JournalsMainContentParams,
    onAction: (JournalsMainContentAction) -> Unit
) {
    when (params.uiState) {
        is JournalsUiState.InitialLoading -> LoadingOverlayView()

        is JournalsUiState.Error ->
            ErrorContentView(
                retryAction = { onAction(JournalsMainContentAction.Retry) },
                message = params.uiState.message
            )

        is JournalsUiState.Content ->
            ContentScreen(
                state =
                    JournalsContentState(
                        journals = params.uiState.journals,
                        isRefreshing = params.isRefreshing,
                        isDeleting = params.isDeleting,
                        ownerConfig = params.ownerConfig
                    ),
                onAction = { action ->
                    when (action) {
                        JournalsContentAction.Refresh -> onAction(JournalsMainContentAction.Refresh)
                        is JournalsContentAction.JournalClick ->
                            onAction(
                                JournalsMainContentAction.JournalClick(
                                    action.params
                                )
                            )
                    }
                }
            )
    }
}

@Composable
private fun JournalsDialogs(
    params: JournalsDialogsParams,
    actions: JournalsDialogsActions
) {
    with(params.state) {
        if (showDeleteDialog && journalToDelete != null) {
            DeleteConfirmationDialog(
                onDismiss = actions.onDeleteDismiss,
                onConfirm = actions.onDeleteConfirm
            )
        }

        journalToEditSettings?.let { journal ->
            val isSavingSettings =
                (params.uiState as? JournalsUiState.Content)?.isSavingJournalSettings ?: false
            JournalSettingsDialog(
                journal = journal,
                onDismiss = actions.onSettingsDismiss,
                viewModel = params.viewModel,
                isSaving = isSavingSettings
            )
        }

        if (showTextEntrySheet && textEntryMode != null) {
            TextEntrySheetHost(
                show = true,
                mode = textEntryMode,
                onDismissed = actions.onTextEntryDismiss,
                onSendSuccess = actions.onTextEntrySuccess
            )
        }
    }
}

/**
 * Обработчик событий ViewModel
 */
@Composable
private fun JournalsEventHandler(
    viewModel: IJournalsViewModel,
    getJournalToEditSettings: () -> Journal?,
    onJournalSettingsSaved: () -> Unit
) {
    LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JournalsEvent.JournalSettingsSaved -> {
                    // Закрыть диалог только если это был наш journal
                    val journalToEditSettings = getJournalToEditSettings()
                    if (journalToEditSettings?.id == event.journal.id) {
                        onJournalSettingsSaved()
                    }
                }
            }
        }
    }
}

/**
 * Контент с Pull-to-Refresh и списком дневников
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(
    state: JournalsContentState,
    onAction: (JournalsContentAction) -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(JournalsContentAction.Refresh) },
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
        if (state.journals.isEmpty()) {
            if (state.ownerConfig.isOwner && !state.isRefreshing) {
                EmptyStateView(
                    text = stringResource(R.string.journals_empty),
                    buttonTitle = stringResource(R.string.create_journal),
                    enabled = !state.isDeleting,
                    onButtonClick = state.ownerConfig.onCreateJournalClick
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                JournalsList(
                    journals = state.journals,
                    enabled = !state.isRefreshing && !state.isDeleting,
                    ownerConfig = state.ownerConfig,
                    onJournalClick = { params -> onAction(JournalsContentAction.JournalClick(params)) }
                )

                if (state.isDeleting) {
                    LoadingOverlayView()
                }
            }
        }
    }
}

/**
 * Список дневников
 */
@Composable
private fun JournalsList(
    journals: List<Journal>,
    enabled: Boolean = true,
    ownerConfig: OwnerDisplayConfig,
    onJournalClick: (JournalNavigationParams) -> Unit
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
        items(items = journals, key = { it.id }) { journal ->
            JournalItem(
                journal = journal,
                enabled = enabled,
                ownerConfig = ownerConfig,
                context = context,
                onClick = onJournalClick
            )
        }
    }
}

@Composable
private fun JournalItem(
    journal: Journal,
    enabled: Boolean,
    ownerConfig: OwnerDisplayConfig,
    context: android.content.Context,
    onClick: (JournalNavigationParams) -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    onClick(
                        JournalNavigationParams(
                            journalId = journal.id,
                            journalOwnerId = journal.ownerId!!,
                            journalTitle = journal.title ?: "",
                            viewAccess = journal.viewAccess?.name ?: JournalAccess.NOBODY.name,
                            commentAccess = journal.commentAccess?.name ?: JournalAccess.NOBODY.name
                        )
                    )
                }
    ) {
        JournalRowView(
            data =
                JournalRowData(
                    modifier = Modifier.fillMaxWidth(),
                    imageStringURL = journal.lastMessageImage,
                    title = journal.title ?: "",
                    dateString = DateFormatter.formatDate(context, journal.lastMessageDate),
                    bodyText = journal.lastMessageText ?: "",
                    mode = JournalRowMode.ROOT,
                    actions =
                        if (ownerConfig.isOwner) {
                            listOf(JournalAction.SETUP, JournalAction.DELETE)
                        } else {
                            emptyList()
                        },
                    onClickAction = { action ->
                        if (action == JournalAction.DELETE) {
                            ownerConfig.onDeleteClick(journal)
                        } else if (action == JournalAction.SETUP) {
                            ownerConfig.onSetupClick(journal)
                        }
                    }
                )
        )
    }
}

/**
 * Диалог подтверждения удаления дневника
 */
@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.delete_journal_title))
        },
        text = {
            Text(stringResource(R.string.delete_journal_message))
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
@Preview(showBackground = true, locale = "ru", name = "Empty Journals List - Owner")
@Composable
private fun JournalsListScreenEmptyPreview() {
    val ownerConfig =
        OwnerDisplayConfig(
            isOwner = true,
            onDeleteClick = {},
            onSetupClick = {},
            onCreateJournalClick = {}
        )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state =
                JournalsContentState(
                    journals = emptyList(),
                    isRefreshing = false,
                    isDeleting = false,
                    ownerConfig = ownerConfig
                ),
            onAction = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, locale = "ru", name = "Empty Journals List - Other User Loading")
@Composable
private fun JournalsListScreenEmptyOtherUserLoadingPreview() {
    val ownerConfig =
        OwnerDisplayConfig(
            isOwner = false,
            onDeleteClick = {},
            onSetupClick = {},
            onCreateJournalClick = {}
        )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state =
                JournalsContentState(
                    journals = emptyList(),
                    isRefreshing = true,
                    isDeleting = false,
                    ownerConfig = ownerConfig
                ),
            onAction = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, locale = "ru", name = "Empty Journals List - Other User Loaded")
@Composable
private fun JournalsListScreenEmptyOtherUserLoadedPreview() {
    val ownerConfig =
        OwnerDisplayConfig(
            isOwner = false,
            onDeleteClick = {},
            onSetupClick = {},
            onCreateJournalClick = {}
        )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state =
                JournalsContentState(
                    journals = emptyList(),
                    isRefreshing = false,
                    isDeleting = false,
                    ownerConfig = ownerConfig
                ),
            onAction = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, locale = "ru", name = "Journals List with Items")
@Composable
private fun JournalsListScreenWithItemsPreview() {
    val sampleJournals =
        listOf(
            Journal(
                id = 1,
                title = "Мой первый дневник",
                lastMessageText = "Сегодня была отличная тренировка!",
                lastMessageDate = "2024-01-15T10:30:00",
                lastMessageImage = null,
                createDate = "2024-01-01T00:00:00",
                modifyDate = "2024-01-15T10:30:00",
                entriesCount = 5,
                ownerId = 1,
                viewAccess = JournalAccess.FRIENDS,
                commentAccess = JournalAccess.FRIENDS
            ),
            Journal(
                id = 2,
                title = "Дневник питания",
                lastMessageText = "Завтрак: овсянка с фруктами",
                lastMessageDate = "2024-01-14T08:00:00",
                lastMessageImage = null,
                createDate = "2024-01-02T00:00:00",
                modifyDate = "2024-01-14T08:00:00",
                entriesCount = 3,
                ownerId = 1,
                viewAccess = JournalAccess.NOBODY,
                commentAccess = JournalAccess.NOBODY
            )
        )

    val ownerConfig =
        OwnerDisplayConfig(
            isOwner = true,
            onDeleteClick = {},
            onSetupClick = {},
            onCreateJournalClick = {}
        )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state =
                JournalsContentState(
                    journals = sampleJournals,
                    isRefreshing = false,
                    isDeleting = false,
                    ownerConfig = ownerConfig
                ),
            onAction = {}
        )
    }
}

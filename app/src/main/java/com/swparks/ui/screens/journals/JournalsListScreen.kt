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
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    data class JournalClick(val params: JournalNavigationParams) : JournalsListAction()
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
    data class JournalClick(val params: JournalNavigationParams) : JournalsContentAction()
}

/**
 * Экран списка дневников пользователя
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием экрана
 * @param config Конфигурация экрана (appState, params, parentPaddingValues)
 * @param onAction Обработчик действий
 */
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

    // Проверка: просмотр собственных дневников или чужих
    val isOwner = config.appState.currentUser?.id == config.params.userId

    // Состояние диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var journalToDelete by remember { mutableStateOf<Journal?>(null) }

    // Состояние диалога настроек дневника
    var journalToEditSettings by remember { mutableStateOf<Journal?>(null) }

    // Состояние для TextEntrySheet
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }

    // Обработчик для действия SETUP (настройки дневника)
    val onSetupClick: (Journal) -> Unit = { journal ->
        journalToEditSettings = journal
    }

    // Обработчик событий ViewModel для закрытия диалога настроек
    JournalsEventHandler(
        viewModel = viewModel,
        getJournalToEditSettings = { journalToEditSettings },
        onJournalSettingsSaved = {
            journalToEditSettings = null
        }
    )

    val ownerConfig = OwnerDisplayConfig(
        isOwner = isOwner,
        onDeleteClick = { journal ->
            journalToDelete = journal
            showDeleteDialog = true
        },
        onSetupClick = onSetupClick,
        onCreateJournalClick = {
            textEntryMode = TextEntryMode.NewJournal(config.params.userId)
            showTextEntrySheet = true
        }
    )

    Scaffold(
        modifier = modifier.padding(bottom = config.parentPaddingValues.calculateBottomPadding()),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.journals_list_title))
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(JournalsListAction.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Показывать FAB только если текущий пользователь авторизован И открывает свой профиль
            when (uiState) {
                is JournalsUiState.Content -> {
                    if (!isDeleting && isOwner) {
                        FloatingActionButton(
                            onClick = ownerConfig.onCreateJournalClick,
                            modifier = Modifier.testTag("CreateJournalFAB")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_entry),
                                contentDescription = stringResource(R.string.fab_create_journal_description)
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = config.parentPaddingValues.calculateStartPadding(layoutDirection),
                    top = config.parentPaddingValues.calculateTopPadding(),
                    end = config.parentPaddingValues.calculateEndPadding(layoutDirection)
                )
                .padding(innerPadding)
        ) {
            when (uiState) {
                is JournalsUiState.InitialLoading -> {
                    LoadingOverlayView()
                }

                is JournalsUiState.Error -> {
                    ErrorContentView(
                        retryAction = { viewModel.retry() },
                        message = (uiState as JournalsUiState.Error).message
                    )
                }

                is JournalsUiState.Content -> {
                    val contentState = uiState as JournalsUiState.Content
                    ContentScreen(
                        state = JournalsContentState(
                            journals = contentState.journals,
                            isRefreshing = isRefreshing,
                            isDeleting = isDeleting,
                            ownerConfig = ownerConfig
                        ),
                        onAction = { action ->
                            when (action) {
                                JournalsContentAction.Refresh -> viewModel.loadJournals()
                                is JournalsContentAction.JournalClick -> onAction(
                                    JournalsListAction.JournalClick(
                                        action.params
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }

        // Диалог подтверждения удаления
        if (showDeleteDialog && journalToDelete != null) {
            DeleteConfirmationDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    journalToDelete?.let { journal ->
                        viewModel.deleteJournal(journal.id)
                    }
                    showDeleteDialog = false
                }
            )
        }

        // Диалог настроек дневника
        journalToEditSettings?.let { journal ->
            val isSavingSettings =
                (uiState as? JournalsUiState.Content)?.isSavingJournalSettings ?: false
            JournalSettingsDialog(
                journal = journal,
                onDismiss = { journalToEditSettings = null },
                viewModel = viewModel,
                isSaving = isSavingSettings
            )
        }

        // TextEntrySheet для создания дневника
        if (showTextEntrySheet && textEntryMode != null) {
            TextEntrySheetHost(
                show = showTextEntrySheet,
                mode = textEntryMode!!,
                onDismissed = {
                    showTextEntrySheet = false
                    textEntryMode = null
                },
                onSendSuccess = {
                    // Обновить список дневников после успешного создания
                    showTextEntrySheet = false
                    textEntryMode = null
                    viewModel.loadJournals()
                }
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
                modifier = Modifier
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
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacing_regular),
            top = dimensionResource(R.dimen.spacing_small),
            end = dimensionResource(R.dimen.spacing_regular),
            bottom = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(
            items = journals,
            key = { it.id }
        ) { journal ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        onJournalClick(
                            JournalNavigationParams(
                                journalId = journal.id,
                                journalOwnerId = journal.ownerId!!,
                                journalTitle = journal.title ?: "",
                                viewAccess = journal.viewAccess?.name ?: JournalAccess.NOBODY.name,
                                commentAccess = journal.commentAccess?.name
                                    ?: JournalAccess.NOBODY.name
                            )
                        )
                    }
            ) {
                JournalRowView(
                    data = JournalRowData(
                        modifier = Modifier.fillMaxWidth(),
                        imageStringURL = journal.lastMessageImage,
                        title = journal.title ?: "",
                        dateString = DateFormatter.formatDate(
                            context = context,
                            dateString = journal.lastMessageDate
                        ),
                        bodyText = journal.lastMessageText ?: "",
                        mode = JournalRowMode.ROOT,
                        // Показывать действия только для владельца
                        actions = if (ownerConfig.isOwner) {
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
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        properties = DialogProperties(
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
    val ownerConfig = OwnerDisplayConfig(
        isOwner = true,
        onDeleteClick = {},
        onSetupClick = {},
        onCreateJournalClick = {}
    )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state = JournalsContentState(
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
    val ownerConfig = OwnerDisplayConfig(
        isOwner = false,
        onDeleteClick = {},
        onSetupClick = {},
        onCreateJournalClick = {}
    )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state = JournalsContentState(
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
    val ownerConfig = OwnerDisplayConfig(
        isOwner = false,
        onDeleteClick = {},
        onSetupClick = {},
        onCreateJournalClick = {}
    )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state = JournalsContentState(
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
    val sampleJournals = listOf(
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

    val ownerConfig = OwnerDisplayConfig(
        isOwner = true,
        onDeleteClick = {},
        onSetupClick = {},
        onCreateJournalClick = {}
    )
    JetpackWorkoutAppTheme {
        ContentScreen(
            state = JournalsContentState(
                journals = sampleJournals,
                isRefreshing = false,
                isDeleting = false,
                ownerConfig = ownerConfig
            ),
            onAction = {}
        )
    }
}

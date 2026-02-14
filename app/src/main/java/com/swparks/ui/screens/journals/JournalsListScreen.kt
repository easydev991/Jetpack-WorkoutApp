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
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.state.JournalsUiState
import com.swparks.ui.viewmodel.IJournalsViewModel
import com.swparks.util.DateFormatter

/**
 * Экран списка дневников пользователя
 *
 * @param modifier Модификатор
 * @param appState Состояние приложения для проверки авторизации
 * @param userId Идентификатор пользователя
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для навигации назад
 * @param onJournalClick Callback для навигации к записям дневника
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalsListScreen(
    modifier: Modifier = Modifier,
    appState: AppState,
    userId: Long,
    viewModel: IJournalsViewModel,
    onBackClick: () -> Unit,
    onJournalClick: (journalId: Long, journalOwnerId: Long, journalTitle: String, commentAccess: String) -> Unit,
    parentPaddingValues: PaddingValues
) {
    val layoutDirection = LocalLayoutDirection.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()

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

    Scaffold(
        modifier = modifier.padding(bottom = parentPaddingValues.calculateBottomPadding()),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.journals_list_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                    val currentUser = appState.currentUser
                    if (!isDeleting && currentUser != null && userId == currentUser.id) {
                        FloatingActionButton(
                            onClick = {
                                // Открыть bottom sheet с режимом создания дневника
                                textEntryMode = TextEntryMode.NewJournal(userId)
                                showTextEntrySheet = true
                            },
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
                    start = parentPaddingValues.calculateStartPadding(layoutDirection),
                    top = parentPaddingValues.calculateTopPadding(),
                    end = parentPaddingValues.calculateEndPadding(layoutDirection)
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
                        journals = contentState.journals,
                        isRefreshing = isRefreshing,
                        isDeleting = isDeleting,
                        onRefresh = { viewModel.loadJournals() },
                        onJournalClick = onJournalClick,
                        onDeleteClick = { journal ->
                            journalToDelete = journal
                            showDeleteDialog = true
                        },
                        onSetupClick = onSetupClick,
                        onCreateJournalClick = {
                            // Открыть bottom sheet для создания дневника
                            textEntryMode = TextEntryMode.NewJournal(userId)
                            showTextEntrySheet = true
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
            JournalSettingsDialog(
                journal = journal,
                onDismiss = { journalToEditSettings = null },
                viewModel = viewModel,
                uiState = uiState
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

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.swparks.ui.viewmodel.JournalsEvent.JournalSettingsSaved -> {
                    // Закрыть диалог только если это был наш journal
                    val journalToEditSettings = getJournalToEditSettings()
                    if (journalToEditSettings?.id == event.journal.id) {
                        onJournalSettingsSaved()
                    }
                }

                is com.swparks.ui.viewmodel.JournalsEvent.ShowSnackbar -> {
                    // Показать Snackbar (TODO: добавить SnackbarHost)
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
    journals: List<Journal>,
    isRefreshing: Boolean,
    isDeleting: Boolean,
    onRefresh: () -> Unit,
    onJournalClick: (journalId: Long, journalOwnerId: Long, journalTitle: String, commentAccess: String) -> Unit,
    onDeleteClick: (Journal) -> Unit,
    onSetupClick: (Journal) -> Unit = {},
    onCreateJournalClick: () -> Unit = {}
) {
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = dimensionResource(R.dimen.spacing_regular))
            )
        }
    ) {
        if (journals.isEmpty()) {
            // Заглушка при пустом списке
            EmptyStateView(
                text = stringResource(R.string.journals_empty),
                buttonTitle = stringResource(R.string.create_journal),
                enabled = !isRefreshing && !isDeleting,
                onButtonClick = onCreateJournalClick
            )
        } else {
            // Список дневников
            Box(modifier = Modifier.fillMaxSize()) {
                JournalsList(
                    journals = journals,
                    enabled = !isRefreshing && !isDeleting,
                    onJournalClick = onJournalClick,
                    onDeleteClick = onDeleteClick,
                    onSetupClick = onSetupClick
                )

                // Индикатор загрузки при удалении
                if (isDeleting) {
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
    onJournalClick: (journalId: Long, journalOwnerId: Long, journalTitle: String, commentAccess: String) -> Unit = { _, _, _, _ -> },
    onDeleteClick: (Journal) -> Unit = { },
    onSetupClick: (Journal) -> Unit = { }
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
                        val journalTitle = journal.title ?: ""
                        val commentAccess = journal.commentAccess?.name ?: JournalAccess.NOBODY.name
                        onJournalClick(journal.id, journal.ownerId!!, journalTitle, commentAccess)
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
                        actions = listOf(
                            JournalAction.SETUP,
                            JournalAction.DELETE
                        ),
                        onClickAction = { action ->
                            if (action == JournalAction.DELETE) {
                                onDeleteClick(journal)
                            } else if (action == JournalAction.SETUP) {
                                onSetupClick(journal)
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
@Preview(showBackground = true, locale = "ru", name = "Empty Journals List")
@Composable
private fun JournalsListScreenEmptyPreview() {
    JetpackWorkoutAppTheme {
        ContentScreen(
            journals = emptyList(),
            isRefreshing = false,
            isDeleting = false,
            onRefresh = {},
            onJournalClick = { _, _, _, _ -> },
            onDeleteClick = {},
            onSetupClick = {},
            onCreateJournalClick = {}
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

    JetpackWorkoutAppTheme {
        ContentScreen(
            journals = sampleJournals,
            isRefreshing = false,
            isDeleting = false,
            onRefresh = {},
            onJournalClick = { _, _, _, _ -> },
            onDeleteClick = {},
            onSetupClick = {},
            onCreateJournalClick = {}
        )
    }
}

@file:Suppress("UnusedPrivateMember")

package com.swparks.ui.screens.journals

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

/**
 * Экран списка записей в дневнике пользователя
 *
 * @param modifier Модификатор
 * @param journalId Идентификатор дневника
 * @param journalTitle Название дневника для заголовка AppBar
 * @param journalOwnerId Идентификатор владельца дневника
 * @param journalViewAccess Уровень доступа для просмотра (из навигации)
 * @param journalCommentAccess Уровень доступа для комментариев (из навигации)
 * @param viewModel ViewModel для управления состоянием экрана
 * @param appState Состояние приложения для проверки текущего пользователя
 * @param onBackClick Callback для навигации назад
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 * @param textEntrySheetHostContent Content для замены TextEntrySheetHost в тестах
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntriesScreen(
    modifier: Modifier = Modifier,
    journalId: Long,
    journalTitle: String,
    journalOwnerId: Long,
    journalViewAccess: String? = null,
    journalCommentAccess: String? = null,
    viewModel: IJournalEntriesViewModel,
    appState: AppState,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues,
    textEntrySheetHostContent: @Composable (
        Boolean,
        TextEntryMode?,
        () -> Unit,
        () -> Unit
    ) -> Unit = { show, mode, onDismissed, onSendSuccess ->
        if (show && mode != null) {
            TextEntrySheetHost(
                show = show,
                mode = mode,
                onDismissed = onDismissed,
                onSendSuccess = onSendSuccess
            )
        }
    }
) {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()

    // Проверка: просмотр собственного дневника или чужого
    val isOwner = appState.currentUser?.id == journalOwnerId

    // Состояние диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Long?>(null) }

    // Состояние для TextEntrySheet
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }

    // Состояние для диалога настроек
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Подписка на события ViewModel для закрытия диалога настроек
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JournalEntriesEvent.JournalSettingsSaved -> {
                    showSettingsDialog = false
                }
            }
        }
    }

    // Получаем текущий дневник из состояния
    val currentJournal = (uiState as? JournalEntriesUiState.Content)?.journal

    Scaffold(
        modifier = modifier.padding(bottom = parentPaddingValues.calculateBottomPadding()),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(currentJournal?.title ?: journalTitle)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Иконка настроек видна только владельцу дневника
                    if (isOwner) {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            when (uiState) {
                is JournalEntriesUiState.Content -> {
                    val contentState = uiState as JournalEntriesUiState.Content
                    if (isOwner && contentState.canCreateEntry && !isDeleting) {
                        FloatingActionButton(
                            onClick = {
                                textEntryMode = TextEntryMode.NewForJournal(
                                    ownerId = journalOwnerId,
                                    journalId = journalId
                                )
                                showTextEntrySheet = true
                            },
                            modifier = Modifier.testTag("AddEntryFAB")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_entry),
                                contentDescription = stringResource(R.string.fab_add_entry_description)
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
                    // Bottom padding пропускаем для FAB
                )
                .padding(innerPadding)
        ) {
            // Важно: LoadingOverlayView рендерится только здесь для InitialLoading/Error
            // Это предотвращает перекрытие FAB при гонке состояний
            when (uiState) {
                is JournalEntriesUiState.InitialLoading -> {
                    LoadingOverlayView()
                }

                is JournalEntriesUiState.Error -> {
                    ErrorContentView(
                        retryAction = { viewModel.retry() },
                        message = (uiState as JournalEntriesUiState.Error).message
                    )
                }

                is JournalEntriesUiState.Content -> {
                    val contentState = uiState as JournalEntriesUiState.Content
                    ContentScreen(
                        entries = contentState.entries,
                        isRefreshing = isRefreshing,
                        isDeleting = isDeleting,
                        canCreateEntry = contentState.canCreateEntry && isOwner,
                        isOwner = isOwner,
                        canReportEntry = false, // Скрыто до доработки функционала жалоб
                        firstEntryId = contentState.firstEntryId,
                        onRefresh = { viewModel.loadEntries() },
                        onDeleteEntry = if (isOwner) {
                            { entryId ->
                                showDeleteDialog = true
                                entryToDelete = entryId
                            }
                        } else {
                            {}
                        },
                        onEditEntry = if (isOwner) {
                            { entry ->
                                textEntryMode = TextEntryMode.EditJournalEntry(
                                    ownerId = journalOwnerId,
                                    editInfo = EditInfo(
                                        parentObjectId = journalId,
                                        entryId = entry.id,
                                        oldEntry = entry.message ?: ""
                                    )
                                )
                                showTextEntrySheet = true
                            }
                        } else {
                            {}
                        },
                        onReportEntry = { entry ->
                            val complaint = com.swparks.util.Complaint.JournalEntry(
                                author = entry.authorName ?: "неизвестен",
                                entryText = entry.message ?: ""
                            )
                            com.swparks.ui.screens.more.sendComplaint(complaint, context)
                        },
                        onAddEntryClick = if (isOwner) {
                            {
                                textEntryMode = TextEntryMode.NewForJournal(
                                    ownerId = journalOwnerId,
                                    journalId = journalId
                                )
                                showTextEntrySheet = true
                            }
                        } else {
                            {}
                        }
                    )
                }
            }
        }
    }

    // Диалог подтверждения удаления записи
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                entryToDelete?.let { viewModel.deleteEntry(it) }
                showDeleteDialog = false
            }
        )
    }

    // TextEntrySheet для создания и редактирования записей
    textEntrySheetHostContent(
        showTextEntrySheet,
        textEntryMode,
        { showTextEntrySheet = false },
        {
            showTextEntrySheet = false
            // Обновляем список только при создании новой записи
            // При редактировании Flow обновляется автоматически через локальный кэш
            val mode = textEntryMode
            if (mode is TextEntryMode.NewForJournal) {
                viewModel.loadEntries()
            }
        }
    )

    // Диалог настроек дневника
    // Используем данные из навигации если currentJournal еще не загружен
    if (showSettingsDialog) {
        val isSavingSettings by viewModel.isSavingSettings.collectAsState()
        val journalForDialog = currentJournal ?: Journal(
            id = journalId,
            title = journalTitle,
            ownerId = journalOwnerId,
            viewAccess = journalViewAccess?.let { JournalAccess.valueOf(it) }
                ?: JournalAccess.NOBODY,
            commentAccess = journalCommentAccess?.let { JournalAccess.valueOf(it) }
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
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel,
            isSaving = isSavingSettings
        )
    }
}

/**
 * Контент с Pull-to-Refresh и списком записей
 *
 * @param canReportEntry Если true - показывать действие REPORT (для чужих дневников)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(
    entries: List<JournalEntry>,
    isRefreshing: Boolean,
    isDeleting: Boolean,
    canCreateEntry: Boolean = true,
    isOwner: Boolean = false,
    canReportEntry: Boolean = false,
    firstEntryId: Long? = null,
    onRefresh: () -> Unit,
    onDeleteEntry: (Long) -> Unit = {},
    onEditEntry: (JournalEntry) -> Unit = {},
    onReportEntry: (JournalEntry) -> Unit = {},
    onAddEntryClick: () -> Unit = {}
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
        if (entries.isEmpty()) {
            // Заглушка при пустом списке записей
            if (canCreateEntry) {
                EmptyStateView(
                    text = stringResource(R.string.entries_empty),
                    buttonTitle = stringResource(R.string.create_entry),
                    enabled = !isRefreshing && !isDeleting,
                    onButtonClick = {
                        onAddEntryClick()
                    }
                )
            }
        } else {
            // Список записей
            EntriesList(
                entries = entries,
                enabled = !isRefreshing && !isDeleting,
                canEditEntry = { entry -> entry.authorId != null && isOwner },
                canDeleteEntry = isOwner,
                canReportEntry = canReportEntry,
                firstEntryId = firstEntryId,
                onDeleteEntry = onDeleteEntry,
                onEditEntry = onEditEntry,
                onReportEntry = onReportEntry
            )
        }

        // Индикатор загрузки при удалении записи (поверх всего)
        if (isDeleting) {
            LoadingOverlayView()
        }
    }
}

/**
 * Список записей в дневнике
 *
 * @param canReportEntry Если true - показывать действие REPORT (для чужих дневников)
 */
@Suppress("UnusedParameter")
@Composable
private fun EntriesList(
    entries: List<JournalEntry>,
    enabled: Boolean = true,
    canEditEntry: (JournalEntry) -> Boolean = { false },
    canDeleteEntry: Boolean = false,
    canReportEntry: Boolean = false,
    firstEntryId: Long? = null,
    onDeleteEntry: (Long) -> Unit = {},
    onEditEntry: (JournalEntry) -> Unit = {},
    onReportEntry: (JournalEntry) -> Unit = {}
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
            items = entries,
            key = { it.id }
        ) { entry ->
            // Определяем доступные действия для записи
            val actions = mutableListOf<JournalAction>()
            if (canEditEntry(entry)) {
                actions.add(JournalAction.EDIT)
            }
            // Первую запись (с минимальным id) нельзя удалить
            // Удалять может только владелец дневника
            if (canDeleteEntry && entry.id != firstEntryId) {
                actions.add(JournalAction.DELETE)
            }
            // Жалоба доступна только для чужих дневников
            if (canReportEntry) {
                actions.add(JournalAction.REPORT)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("JournalEntry_${entry.id}")
            ) {
                JournalRowView(
                    data = JournalRowData(
                        modifier = Modifier.fillMaxWidth(),
                        imageStringURL = entry.authorImage,
                        title = entry.authorName ?: "",
                        dateString = DateFormatter.formatDate(
                            context = context,
                            dateString = entry.createDate
                        ),
                        bodyText = entry.message ?: "",
                        mode = JournalRowMode.ENTRY,
                        enabled = enabled,
                        actions = actions,
                        onClickAction = { action ->
                            when (action) {
                                JournalAction.EDIT -> {
                                    onEditEntry(entry)
                                }

                                JournalAction.DELETE -> {
                                    onDeleteEntry(entry.id)
                                }

                                JournalAction.REPORT -> {
                                    onReportEntry(entry)
                                }

                                else -> {
                                }
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
@Preview(showBackground = true, locale = "ru", name = "Empty Journal Entries")
@Composable
private fun JournalEntriesScreenEmptyPreview() {
    JetpackWorkoutAppTheme {
        ContentScreen(
            entries = emptyList(),
            isRefreshing = false,
            isDeleting = false,
            canCreateEntry = true,
            isOwner = true,
            firstEntryId = null,
            onRefresh = {},
            onDeleteEntry = {},
            onEditEntry = {},
            onAddEntryClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, locale = "ru", name = "Journal Entries with Items")
@Composable
private fun JournalEntriesScreenWithItemsPreview() {
    val sampleEntries = listOf(
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
            entries = sampleEntries,
            isRefreshing = false,
            isDeleting = false,
            canCreateEntry = true,
            isOwner = true,
            firstEntryId = 1,
            onRefresh = {},
            onDeleteEntry = {},
            onEditEntry = {},
            onAddEntryClick = {}
        )
    }
}

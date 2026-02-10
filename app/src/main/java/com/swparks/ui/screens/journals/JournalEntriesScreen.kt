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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.domain.model.JournalEntry
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.JournalAction
import com.swparks.ui.ds.JournalRowData
import com.swparks.ui.ds.JournalRowMode
import com.swparks.ui.ds.JournalRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.model.EditInfo
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.ui.viewmodel.IJournalEntriesViewModel
import com.swparks.util.DateFormatter
import kotlinx.coroutines.launch

/**
 * Экран списка записей в дневнике пользователя
 *
 * @param modifier Модификатор
 * @param journalId Идентификатор дневника
 * @param journalTitle Название дневника для заголовка AppBar
 * @param journalOwnerId Идентификатор владельца дневника
 * @param viewModel ViewModel для управления состоянием экрана
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
    viewModel: IJournalEntriesViewModel,
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
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val scope = rememberCoroutineScope()

    // Snackbar для отображения сообщений
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояние диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Long?>(null) }

    // Состояние для TextEntrySheet
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var textEntryMode by remember { mutableStateOf<TextEntryMode?>(null) }

    // Подписка на события ViewModel для Snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.swparks.ui.viewmodel.JournalEntriesEvent.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.padding(bottom = parentPaddingValues.calculateBottomPadding()),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(journalTitle)
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            when (uiState) {
                is JournalEntriesUiState.Content -> {
                    val contentState = uiState as JournalEntriesUiState.Content
                    if (contentState.canCreateEntry && !isDeleting) {
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
                        canCreateEntry = contentState.canCreateEntry,
                        firstEntryId = contentState.firstEntryId,
                        onRefresh = { viewModel.loadEntries() },
                        onDeleteEntry = { entryId ->
                            showDeleteDialog = true
                            entryToDelete = entryId
                        },
                        onEditEntry = { entry ->
                            textEntryMode = TextEntryMode.EditJournalEntry(
                                ownerId = journalOwnerId,
                                editInfo = EditInfo(
                                    parentObjectId = journalId,
                                    entryId = entry.id,
                                    oldEntry = entry.message ?: ""
                                )
                            )
                            showTextEntrySheet = true
                        },
                        onAddEntryClick = {
                            textEntryMode = TextEntryMode.NewForJournal(
                                ownerId = journalOwnerId,
                                journalId = journalId
                            )
                            showTextEntrySheet = true
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
}

/**
 * Контент с Pull-to-Refresh и списком записей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(
    entries: List<JournalEntry>,
    isRefreshing: Boolean,
    isDeleting: Boolean,
    canCreateEntry: Boolean = true,
    firstEntryId: Long? = null,
    onRefresh: () -> Unit,
    onDeleteEntry: (Long) -> Unit = {},
    onEditEntry: (JournalEntry) -> Unit = {},
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
        // Индикатор загрузки для pull-to-refresh (перекрывает только контент)
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = dimensionResource(R.dimen.spacing_regular),
                        top = dimensionResource(R.dimen.spacing_small),
                        end = dimensionResource(R.dimen.spacing_regular),
                        bottom = dimensionResource(R.dimen.spacing_regular)
                    ),
                contentAlignment = Alignment.Center
            ) {
                LoadingOverlayView()
            }
        }

        if (entries.isEmpty()) {
            // Заглушка при пустом списке записей
            EmptyStateView(
                text = stringResource(R.string.entries_empty),
                buttonTitle = stringResource(R.string.create_entry),
                enabled = canCreateEntry && !isRefreshing && !isDeleting,
                onButtonClick = {
                    onAddEntryClick()
                }
            )
        } else {
            // Список записей
            EntriesList(
                entries = entries,
                enabled = !isRefreshing && !isDeleting,
                canEditEntry = { entry -> entry.authorId != null },
                firstEntryId = firstEntryId,
                onDeleteEntry = onDeleteEntry,
                onEditEntry = onEditEntry
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
 */
@Composable
private fun EntriesList(
    entries: List<JournalEntry>,
    enabled: Boolean = true,
    canEditEntry: (JournalEntry) -> Boolean = { false },
    firstEntryId: Long? = null,
    onDeleteEntry: (Long) -> Unit = {},
    onEditEntry: (JournalEntry) -> Unit = {}
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
            if (entry.id != firstEntryId) {
                actions.add(JournalAction.DELETE)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("JournalEntry_${entry.id}")
                    .clickable(enabled = enabled) {
                    }
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
                        actions = actions,
                        onClickAction = { action ->
                            when (action) {
                                JournalAction.EDIT -> {
                                    onEditEntry(entry)
                                }

                                JournalAction.DELETE -> {
                                    onDeleteEntry(entry.id)
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

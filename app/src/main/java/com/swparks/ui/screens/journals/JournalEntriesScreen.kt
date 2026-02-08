package com.swparks.ui.screens.journals

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
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
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для навигации назад
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntriesScreen(
    modifier: Modifier = Modifier,
    journalId: Long,
    journalTitle: String,
    viewModel: IJournalEntriesViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues
) {
    // Логирование запуска экрана
    LaunchedEffect(journalId) {
        Log.i("JournalEntriesScreen", "Экран записей дневника: journalId=$journalId")
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val scope = rememberCoroutineScope()

    // Snackbar для отображения сообщений
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояние диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Long?>(null) }

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
        modifier = modifier,
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues)
                .padding(innerPadding)
        ) {
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
                        onRefresh = { viewModel.loadEntries() },
                        canDeleteEntry = { entryId ->
                            // Проверяем, что это не первая запись
                            entryId != contentState.firstEntryId
                        },
                        onEntryAction = { entry, action ->
                            if (action == JournalAction.DELETE) {
                                showDeleteDialog = true
                                entryToDelete = entry.id
                            }
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
    onRefresh: () -> Unit,
    canDeleteEntry: (Long) -> Boolean = { true },
    onEntryAction: (JournalEntry, JournalAction) -> Unit = { _, _ -> }
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
            EmptyStateView(
                text = stringResource(R.string.entries_empty),
                buttonTitle = stringResource(R.string.create_entry),
                enabled = !isRefreshing && !isDeleting,
                onButtonClick = {
                    Log.i("JournalEntriesScreen", "Нажата кнопка: создать запись")
                }
            )
        } else {
            // Список записей
            EntriesList(
                entries = entries,
                enabled = !isRefreshing && !isDeleting,
                canDeleteEntry = canDeleteEntry,
                onEntryAction = onEntryAction
            )
        }

        // Индикатор загрузки при удалении записи
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
    canDeleteEntry: (Long) -> Boolean = { true },
    onEntryAction: (JournalEntry, JournalAction) -> Unit = { _, _ -> }
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
            val actions = mutableListOf(JournalAction.EDIT)
            if (canDeleteEntry(entry.id)) {
                actions.add(JournalAction.DELETE)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("JournalEntry_${entry.id}")
                    .clickable(enabled = enabled) {
                        Log.i("JournalEntriesScreen", "Нажатие на запись: ${entry.id}")
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
                            Log.i(
                                "JournalEntriesScreen",
                                "Действие: $action для записи: ${entry.id}"
                            )
                            onEntryAction(entry, action)
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

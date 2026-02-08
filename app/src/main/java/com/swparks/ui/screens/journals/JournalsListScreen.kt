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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.JournalAction
import com.swparks.ui.ds.JournalRowData
import com.swparks.ui.ds.JournalRowMode
import com.swparks.ui.ds.JournalRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.state.JournalsUiState
import com.swparks.ui.viewmodel.IJournalsViewModel
import com.swparks.util.DateFormatter

/**
 * Экран списка дневников пользователя
 *
 * @param modifier Модификатор
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
    userId: Long,
    viewModel: IJournalsViewModel,
    onBackClick: () -> Unit,
    onJournalClick: (journalId: Long, journalTitle: String) -> Unit,
    parentPaddingValues: PaddingValues
) {
    // Перезапуск загрузки при смене пользователя
    LaunchedEffect(userId) {
        Log.i("JournalsListScreen", "Перезапуск загрузки для пользователя: $userId")
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()

    // Состояние диалога подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }
    var journalToDelete by remember { mutableStateOf<Journal?>(null) }

    Scaffold(
        modifier = modifier,
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues)
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
    onJournalClick: (journalId: Long, journalTitle: String) -> Unit,
    onDeleteClick: (Journal) -> Unit
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
                onButtonClick = {
                    Log.i("JournalsListScreen", "Нажата кнопка: создать дневник")
                }
            )
        } else {
            // Список дневников
            Box(modifier = Modifier.fillMaxSize()) {
                JournalsList(
                    journals = journals,
                    enabled = !isRefreshing && !isDeleting,
                    onJournalClick = onJournalClick,
                    onDeleteClick = onDeleteClick
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
    onJournalClick: (journalId: Long, journalTitle: String) -> Unit = { _, _ -> },
    onDeleteClick: (Journal) -> Unit = { }
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
                        onJournalClick(journal.id, journalTitle)
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
                            Log.i(
                                "JournalsListScreen",
                                "Действие: $action для дневника: ${journal.id}"
                            )
                            if (action == JournalAction.DELETE) {
                                onDeleteClick(journal)
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

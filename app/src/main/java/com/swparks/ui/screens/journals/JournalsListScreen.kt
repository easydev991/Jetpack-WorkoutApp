package com.swparks.ui.screens.journals

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.domain.model.Journal
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
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalsListScreen(
    modifier: Modifier = Modifier,
    userId: Long,
    viewModel: IJournalsViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues
) {
    // Перезапуск загрузки при смене пользователя
    LaunchedEffect(userId) {
        Log.i("JournalsListScreen", "Перезапуск загрузки для пользователя: $userId")
    }

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

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
                        onRefresh = { viewModel.loadJournals() }
                    )
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
    onRefresh: () -> Unit
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
        Box(modifier = Modifier.fillMaxSize()) {
            if (journals.isEmpty()) {
                // Заглушка при пустом списке
                EmptyStateView(enabled = !isRefreshing)
            } else {
                // Список дневников
                JournalsList(
                    journals = journals,
                    enabled = !isRefreshing
                )
            }
            // Индикатор загрузки поверх контента при обновлении
            if (isRefreshing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .matchParentSize()
                )
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
    enabled: Boolean = true
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
                        Log.i("JournalsListScreen", "Нажатие на дневник: ${journal.id}")
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
                            Log.i("JournalsListScreen", "Действие: $action для дневника: ${journal.id}")
                        }
                    )
                )
            }
        }
    }
}

/**
 * Заглушка при пустом списке дневников
 */
@Composable
private fun EmptyStateView(enabled: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.journals_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                enabled = enabled,
                onClick = {
                    Log.i("JournalsListScreen", "Нажата кнопка: создать дневник")
                }
            ) {
                Text(text = stringResource(R.string.create_journal))
            }
        }
    }
}

package com.swparks.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.ui.screen.components.ParksListView
import com.swparks.ui.viewmodel.UserTrainingParksUiState
import com.swparks.ui.viewmodel.UserTrainingParksViewModel

/**
 * Экран для отображения списка площадок, на которых тренируется пользователь
 *
 * @param modifier Modifier для настройки внешнего вида
 * @param viewModel ViewModel для управления данными экрана
 * @param onBackClick Замыкание, вызываемое при нажатии кнопки "Назад"
 * @param parentPaddingValues PaddingValues для соблюдения безопасных зон
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTrainingParksScreen(
    modifier: Modifier = Modifier,
    viewModel: UserTrainingParksViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(onBackClick)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshParks() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues),
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
            Box(modifier = Modifier.padding(innerPadding)) {
                UserTrainingParksContent(uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.where_trains)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTrainingParksContent(uiState: UserTrainingParksUiState) {
    when (uiState) {
        UserTrainingParksUiState.Loading -> LoadingContent()

        is UserTrainingParksUiState.Success -> SuccessContent(uiState.parks)

        is UserTrainingParksUiState.Error -> ErrorContent(uiState.message)
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SuccessContent(parks: List<Park>) {
    if (parks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_parks_found))
        }
    } else {
        ParksListView(
            parks = parks,
            onParkClick = { park ->
                Log.d("UserTrainingParksScreen", "Нажата площадка: ${park.name}")
            }
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message)
    }
}

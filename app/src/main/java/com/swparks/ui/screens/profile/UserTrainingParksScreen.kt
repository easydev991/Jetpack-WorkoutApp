package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
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
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.ParksListView
import com.swparks.ui.viewmodel.IUserTrainingParksViewModel
import com.swparks.ui.viewmodel.UserTrainingParksUiState

data class UserTrainingParksSelectionConfig(
    val selectionMode: Boolean = false,
    val onParkSelected: ((Long, String) -> Unit)? = null
)

data class UserTrainingParksConfig(
    val onParkClick: (Park) -> Unit = { },
    val selectionConfig: UserTrainingParksSelectionConfig = UserTrainingParksSelectionConfig()
)

/**
 * Экран для отображения списка площадок, на которых тренируется пользователь
 *
 * @param modifier Modifier для настройки внешнего вида
 * @param viewModel ViewModel для управления данными экрана
 * @param onBackClick Замыкание, вызываемое при нажатии кнопки "Назад"
 * @param parentPaddingValues PaddingValues для соблюдения безопасных зон
 * @param config Конфигурация экрана - [UserTrainingParksConfig]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTrainingParksScreen(
    modifier: Modifier = Modifier,
    viewModel: IUserTrainingParksViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: androidx.compose.foundation.layout.PaddingValues,
    config: UserTrainingParksConfig = UserTrainingParksConfig()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val handleParkClick: (Park) -> Unit = { park ->
        if (config.selectionConfig.selectionMode && config.selectionConfig.onParkSelected != null) {
            config.selectionConfig.onParkSelected(park.id, park.name)
        } else {
            config.onParkClick(park)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(onBackClick, config.selectionConfig.selectionMode)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshParks() },
            state = pullRefreshState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(parentPaddingValues),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = dimensionResource(R.dimen.spacing_regular))
                )
            }
        ) {
            Box(modifier = Modifier.padding(innerPadding)) {
                UserTrainingParksContent(uiState, handleParkClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    onBackClick: () -> Unit,
    selectionMode: Boolean = false
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                stringResource(
                    if (selectionMode) R.string.your_parks else R.string.where_trains
                )
            )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTrainingParksContent(
    uiState: UserTrainingParksUiState,
    onParkClick: (Park) -> Unit
) {
    when (uiState) {
        UserTrainingParksUiState.Loading -> LoadingOverlayView()

        is UserTrainingParksUiState.Success -> SuccessContent(uiState.parks, onParkClick)

        is UserTrainingParksUiState.Error -> ErrorContent(uiState.message)
    }
}

@Composable
private fun SuccessContent(
    parks: List<Park>,
    onParkClick: (Park) -> Unit
) {
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
            onParkClick = onParkClick
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

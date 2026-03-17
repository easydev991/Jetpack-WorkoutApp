package com.swparks.ui.screens.parks

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
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
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.ParksListView
import com.swparks.ui.viewmodel.IUserAddedParksViewModel
import com.swparks.ui.viewmodel.UserAddedParksUiState

/**
 * Экран для отображения добавленных пользователем площадок
 *
 * @param viewModel ViewModel экрана добавленных площадок
 * @param onBackClick Замыкание для навигации назад
 * @param onParkClick Замыкание, вызываемое при клике на площадку
 * @param modifier Modifier для настройки внешнего вида
 * @param parentPaddingValues Родительские отступы для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksAddedByUserScreen(
    modifier: Modifier = Modifier,
    viewModel: IUserAddedParksViewModel,
    onBackClick: () -> Unit,
    onParkClick: (Park) -> Unit = { park ->
        Log.d("ParksAddedByUserScreen", "Нажата площадка: ${park.name}")
    },
    parentPaddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.added_parks))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(parentPaddingValues)
                .padding(innerPadding),
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
            when (val state = uiState) {
                UserAddedParksUiState.Loading -> LoadingOverlayView()
                is UserAddedParksUiState.Success -> {
                    ParksListView(
                        parks = state.parks,
                        onParkClick = onParkClick,
                        enabled = !isRefreshing
                    )
                }

                is UserAddedParksUiState.Error -> {
                    ErrorContentView(
                        retryAction = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                        message = state.message
                    )
                }
            }
        }
    }
}

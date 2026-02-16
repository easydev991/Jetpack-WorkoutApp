package com.swparks.ui.screens.profile

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.swparks.R
import com.swparks.ui.ds.ErrorContentView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.ds.UserRowView
import com.swparks.ui.state.SearchUserUiState
import com.swparks.ui.viewmodel.ISearchUserViewModel

/**
 * Экран поиска пользователей.
 *
 * @param modifier Модификатор
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для навигации назад
 * @param onUserClick Callback для навигации на профиль найденного пользователя
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    modifier: Modifier = Modifier,
    viewModel: ISearchUserViewModel,
    onBackClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    parentPaddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    SearchUserScreenContent(
        modifier = modifier,
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.searchQuery.value = it },
        onSearch = { viewModel.onSearch() },
        onUserClick = onUserClick,
        onBackClick = onBackClick,
        onRetry = { viewModel.onSearch() },
        parentPaddingValues = parentPaddingValues
    )
}

/**
 * Контент экрана поиска пользователей.
 *
 * Выделен для использования в Preview и тестах без зависимости от ViewModel.
 *
 * @param modifier Модификатор
 * @param uiState Текущее состояние UI
 * @param searchQuery Текущий поисковый запрос
 * @param onSearchQueryChange Callback при изменении поискового запроса
 * @param onSearch Callback при выполнении поиска
 * @param onUserClick Callback при клике на пользователя
 * @param onBackClick Callback для навигации назад
 * @param onRetry Callback при повторной попытке после ошибки
 * @param parentPaddingValues Паддинги для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreenContent(
    modifier: Modifier = Modifier,
    uiState: SearchUserUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onUserClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    parentPaddingValues: PaddingValues
) {
    // Сохраняем последний "показываемый" контент (не Loading)
    // Это позволяет отображать предыдущий контент под LoadingOverlayView
    var lastVisibleState by remember { mutableStateOf<SearchUserUiState>(SearchUserUiState.Initial) }

    LaunchedEffect(uiState) {
        if (uiState !is SearchUserUiState.Loading) {
            lastVisibleState = uiState
        }
    }

    // Определяем, какое состояние отображать:
    // - Если текущее состояние Loading - показываем предыдущее (lastVisibleState)
    // - Иначе показываем текущее состояние напрямую (важно для Preview)
    val displayState = if (uiState is SearchUserUiState.Loading) {
        lastVisibleState
    } else {
        uiState
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.search_users))
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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                // Текстовое поле всегда видимо
                SWTextField(
                    config = TextFieldConfig(
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular)),
                        text = searchQuery,
                        labelID = R.string.username_in_english,
                        supportingText = stringResource(R.string.search_min_length_hint),
                        onTextChange = onSearchQueryChange,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() })
                    )
                )

                // Контент в зависимости от отображаемого состояния
                when (val state = displayState) {
                    is SearchUserUiState.Initial -> {
                        // Пустой экран - ничего не отображаем
                    }

                    is SearchUserUiState.Success -> {
                        UsersList(
                            users = state.users,
                            onUserClick = onUserClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is SearchUserUiState.Empty -> {
                        EmptyResultMessage(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimensionResource(R.dimen.spacing_large))
                        )
                    }

                    is SearchUserUiState.NetworkError -> {
                        ErrorContentView(
                            retryAction = onRetry,
                            message = stringResource(R.string.search_network_error),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is SearchUserUiState.Loading -> {
                        // Это состояние не должно попадать в lastVisibleState
                    }
                }
            }

            // LoadingOverlayView поверх всего контента при Loading
            if (uiState is SearchUserUiState.Loading) {
                LoadingOverlayView()
            }
        }
    }
}

/**
 * Список найденных пользователей.
 */
@Composable
private fun UsersList(
    users: List<com.swparks.data.model.User>,
    onUserClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacing_regular),
            top = dimensionResource(R.dimen.spacing_small),
            end = dimensionResource(R.dimen.spacing_regular),
            bottom = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(
            items = users,
            key = { it.id }
        ) { user ->
            Box(
                modifier = Modifier.clickable { onUserClick(user.id) }
            ) {
                UserRowView(
                    modifier = Modifier.fillMaxWidth(),
                    imageStringURL = user.image,
                    name = user.name,
                    address = null
                )
            }
        }
    }
}

/**
 * Сообщение об отсутствии результатов.
 */
@Composable
private fun EmptyResultMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.user_not_found),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

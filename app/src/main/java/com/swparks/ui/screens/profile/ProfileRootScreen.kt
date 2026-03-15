package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.AppContainer
import com.swparks.navigation.AppState
import com.swparks.navigation.Screen
import com.swparks.ui.ds.AddedParksButton
import com.swparks.ui.ds.BlacklistButton
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.FriendsButton
import com.swparks.ui.ds.IncognitoProfileView
import com.swparks.ui.ds.JournalsButton
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.UsedParksButton
import com.swparks.ui.ds.UserProfileCardView
import com.swparks.ui.ds.UserProfileData
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IProfileViewModel
import com.swparks.ui.viewmodel.ProfileUiState
import kotlinx.coroutines.launch

data class ProfileRootConfig(
    val appContainer: AppContainer? = null,
    val appState: AppState? = null
)

sealed class ProfileAuthAction {
    data object ShowLoginSheet : ProfileAuthAction()
    data object ShowRegisterSheet : ProfileAuthAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRootScreen(
    modifier: Modifier = Modifier,
    viewModel: IProfileViewModel,
    config: ProfileRootConfig,
    onAuthAction: (ProfileAuthAction) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Состояние для показа/скрытия AlertDialog логаута
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Получаем currentUser из ViewModel
    val currentUser by viewModel.currentUser.collectAsState()

    // Получаем UI State из ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Получаем состояние обновления (isRefreshing)
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Получаем состояние загрузки профиля после авторизации
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()

    // Получаем черный список пользователя
    val blacklist by viewModel.blacklist.collectAsState()

    val user = currentUser
    if (isLoadingProfile) {
        // Загрузка профиля после авторизации - показываем LoadingOverlayView
        LoadingOverlayView(modifier = modifier.fillMaxSize())
    } else if (user == null) {
        // Не авторизован - показываем IncognitoProfileView
        IncognitoProfileView(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_regular),
                    end = dimensionResource(R.dimen.spacing_regular)
                ),
            onClickAuth = { onAuthAction(ProfileAuthAction.ShowLoginSheet) },
            onClickRegister = { onAuthAction(ProfileAuthAction.ShowRegisterSheet) }
        )
    } else {
        // Авторизован - показываем профиль и кнопки навигации
        Box(modifier = modifier.fillMaxSize()) {
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshProfile() },
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = dimensionResource(R.dimen.spacing_regular),
                            end = dimensionResource(R.dimen.spacing_regular),
                            top = dimensionResource(R.dimen.spacing_regular),
                            bottom = dimensionResource(R.dimen.spacing_regular)
                        ),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
                ) {
                    // Карточка профиля пользователя
                    // Извлекаем country и city из состояния (Success и Error имеют эти поля)
                    val (country, city) = when (val state = uiState) {
                        is ProfileUiState.Success -> state.country to state.city
                        is ProfileUiState.Error -> state.country to state.city
                        ProfileUiState.Loading -> null to null
                    }

                    // shortAddress: Loading -> "Загрузка...", Success/Error -> страна и город
                    val shortAddress = if (uiState is ProfileUiState.Loading) {
                        stringResource(R.string.loading)
                    } else {
                        "${country?.name ?: ""}, " + (city?.name ?: "")
                    }

                    UserProfileCardView(
                        data = UserProfileData(
                            modifier = Modifier,
                            imageStringURL = user.image,
                            userName = user.fullName ?: user.name,
                            gender = user.genderOption?.let { stringResource(id = it.description) }
                                ?: "",
                            age = user.age,
                            shortAddress = shortAddress
                        )
                    )

                    // Кнопка "Изменить профиль"
                    EditProfileButton(
                        onClick = {
                            config.appState?.navController?.navigate(Screen.EditProfile.route)
                        },
                        enabled = !isRefreshing
                    )

                    // Кнопка "Друзья"
                    if (user.hasFriends || (user.friendRequestCount?.toIntOrNull() ?: 0) > 0) {
                        FriendsButton(
                            friendsCount = user.friendsCount ?: 0,
                            friendRequestsCount = user.friendRequestCount?.toIntOrNull() ?: 0,
                            onClick = {
                                config.appState?.navController?.navigate(Screen.MyFriends.route)
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Кнопка "Где тренируется"
                    if (user.hasUsedParks) {
                        UsedParksButton(
                            parksCount = user.parksCount?.toIntOrNull() ?: 0,
                            onClick = {
                                config.appState?.navController?.navigate(
                                    Screen.UserTrainingParks.createRoute(user.id)
                                )
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Кнопка "Добавленные площадки"
                    if (user.hasAddedParks) {
                        AddedParksButton(
                            addedParksCount = user.addedParks?.size ?: 0,
                            onClick = {
                                config.appState?.navController?.navigate(
                                    Screen.UserParks.createRoute(user.id)
                                )
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Кнопка "Дневники" (всегда показываем для главного пользователя)
                    JournalsButton(
                        journalsCount = user.journalCount ?: 0,
                        onClick = {
                            config.appState?.navController?.navigate(
                                Screen.JournalsList.createRoute(user.id)
                            )
                        },
                        enabled = !isRefreshing
                    )

                    // Кнопка "Черный список" - показываем только если черный список не пустой
                    if (blacklist.isNotEmpty()) {
                        BlacklistButton(
                            blacklistCount = blacklist.size,
                            onClick = {
                                config.appState?.navController?.navigate(Screen.Blacklist.route)
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Кнопка "Где тренируется"
                    if (user.hasUsedParks) {
                        UsedParksButton(
                            parksCount = user.parksCount?.toIntOrNull() ?: 0,
                            onClick = {
                                config.appState?.navController?.navigate(
                                    Screen.UserTrainingParks.createRoute(user.id)
                                )
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Кнопка "Добавленные площадки"
                    if (user.hasAddedParks) {
                        AddedParksButton(
                            addedParksCount = user.addedParks?.size ?: 0,
                            onClick = {
                                config.appState?.navController?.navigate(
                                    Screen.UserParks.createRoute(user.id)
                                )
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Кнопка "Дневники" (всегда показываем для главного пользователя)
                    JournalsButton(
                        journalsCount = user.journalCount ?: 0,
                        onClick = {
                            config.appState?.navController?.navigate(
                                Screen.JournalsList.createRoute(user.id)
                            )
                        },
                        enabled = !isRefreshing
                    )

                    // Кнопка "Черный список" - показываем только если черный список не пустой
                    if (blacklist.isNotEmpty()) {
                        BlacklistButton(
                            blacklistCount = blacklist.size,
                            onClick = {
                                config.appState?.navController?.navigate(Screen.Blacklist.route)
                            },
                            enabled = !isRefreshing
                        )
                    }

                    // Spacer прижимает кнопку выхода к низу экрана
                    Spacer(modifier = Modifier.weight(1f))

                    // Кнопка "Выйти"
                    LogoutButton(
                        onClick = {
                            showLogoutDialog = true
                        },
                        enabled = !isRefreshing
                    )
                }
            }

            // Показываем LoadingOverlayView при первой загрузке (не при pull-to-refresh)
            if (uiState is ProfileUiState.Loading && !isRefreshing) {
                LoadingOverlayView()
            }
        }

        // AlertDialog для подтверждения логаута
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(text = stringResource(id = R.string.logout_confirmation_title))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                config.appContainer?.logoutUseCase?.invoke()
                            }
                            showLogoutDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = stringResource(id = R.string.logout))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(
    appState: AppState,
    onSearchUsersClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.profile))
        },
        actions = {
            if (appState.isAuthorized) {
                IconButton(onClick = onSearchUsersClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(id = R.string.profile)
                    )
                }
            }
        }
    )
}

/**
 * Кнопка "Изменить профиль"
 */
@Composable
private fun EditProfileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SWButton(
        config = ButtonConfig(
            modifier = modifier.fillMaxWidth(),
            size = SWButtonSize.LARGE,
            mode = SWButtonMode.TINTED,
            text = stringResource(id = R.string.edit_profile),
            enabled = enabled,
            onClick = onClick
        )
    )
}

/**
 * Кнопка выхода из учетной записи
 */
@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(stringResource(id = R.string.logout))
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ProfileRootScreenPreview() {
    JetpackWorkoutAppTheme {
        // Для preview используем простой placeholder viewModel без реальных данных
        // В реальном приложении неавторизованный пользователь будет показан через currentUser = null
        IncognitoProfileView(
            modifier = Modifier.fillMaxWidth(),
            onClickAuth = {},
            onClickRegister = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ProfileRootScreenLoggedInPreview() {
    // Preview для авторизованного пользователя требует mock-реализацию ProfileViewModel
    // В реальном приложении Preview будет работать через test doubles
    JetpackWorkoutAppTheme {
        // Для простоты показываем статический вид
        UserProfileCardView(
            data = UserProfileData(
                modifier = Modifier,
                imageStringURL = "",
                userName = "Test User",
                gender = "Мужской",
                age = 34,
                shortAddress = "Россия, Москва"
            )
        )
    }
}


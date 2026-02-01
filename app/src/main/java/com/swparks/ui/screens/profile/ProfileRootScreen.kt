package com.swparks.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.AppContainer
import com.swparks.navigation.AppState
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.FormRowView
import com.swparks.ui.ds.IncognitoProfileView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.UserProfileCardView
import com.swparks.ui.ds.UserProfileData
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.viewmodel.ProfileUiState
import com.swparks.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileRootScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    appContainer: AppContainer? = null,
    onShowLoginSheet: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // Состояние для показа/скрытия AlertDialog логаута
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Получаем currentUser из ViewModel
    val currentUser by viewModel.currentUser.collectAsState()

    // Получаем UI State из ViewModel
    val uiState by viewModel.uiState.collectAsState()

    val user = currentUser
    if (user == null) {
        // Не авторизован - показываем IncognitoProfileView
        IncognitoProfileView(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_regular),
                    end = dimensionResource(R.dimen.spacing_regular)
                ),
            onClickAuth = onShowLoginSheet
        )
    } else {
        // Авторизован - показываем профиль и кнопки навигации
        Column(
            modifier = modifier
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
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    UserProfileCardView(
                        data = UserProfileData(
                            modifier = Modifier,
                            imageStringURL = user.image,
                            userName = user.fullName ?: user.name,
                            gender = user.genderOption?.let { stringResource(id = it.description) }
                                ?: "",
                            age = user.age,
                            shortAddress = "Загрузка..."
                        )
                    )
                }

                is ProfileUiState.Success -> {
                    UserProfileCardView(
                        data = UserProfileData(
                            modifier = Modifier,
                            imageStringURL = user.image,
                            userName = user.fullName ?: user.name,
                            gender = user.genderOption?.let { stringResource(id = it.description) }
                                ?: "",
                            age = user.age,
                            shortAddress = "${state.country?.name ?: ""}, ${state.city?.name ?: ""}"
                        )
                    )
                }

                is ProfileUiState.Error -> {
                    UserProfileCardView(
                        data = UserProfileData(
                            modifier = Modifier,
                            imageStringURL = user.image,
                            userName = user.fullName ?: user.name,
                            gender = user.genderOption?.let { stringResource(id = it.description) }
                                ?: "",
                            age = user.age,
                            shortAddress = state.message
                        )
                    )
                }
            }

            // Кнопка "Изменить профиль"
            EditProfileButton(
                onClick = {
                    Log.i("ProfileRootScreen", "Нажата кнопка: Изменить профиль")
                }
            )

            // Кнопка "Друзья"
            if (user.hasFriends || (user.friendRequestCount?.toIntOrNull() ?: 0) > 0) {
                FriendsButton(
                    friendsCount = user.friendsCount ?: 0,
                    friendRequestsCount = user.friendRequestCount?.toIntOrNull() ?: 0,
                    onClick = {
                        Log.i("ProfileRootScreen", "Нажата кнопка: Друзья")
                    }
                )
            }

            // Кнопка "Где тренируется"
            if (user.hasUsedParks) {
                UsedParksButton(
                    parksCount = user.parksCount?.toIntOrNull() ?: 0,
                    onClick = {
                        Log.i("ProfileRootScreen", "Нажата кнопка: Где тренируется")
                    }
                )
            }

            // Кнопка "Добавленные площадки"
            if (user.hasAddedParks) {
                AddedParksButton(
                    addedParksCount = user.addedParks?.size ?: 0,
                    onClick = {
                        Log.i("ProfileRootScreen", "Нажата кнопка: Добавленные площадки")
                    }
                )
            }

            // Кнопка "Дневники" (всегда показываем для главного пользователя)
            JournalsButton(
                journalsCount = user.journalCount ?: 0,
                onClick = {
                    Log.i("ProfileRootScreen", "Нажата кнопка: Дневники")
                }
            )

            // Кнопка "Черный список" - временно скрыта
            // @Suppress("ForbiddenComment")
            // TODO: Реализовать после интеграции с реальным списком черного списка
            // BlacklistButton(
            //     onClick = {
            //         Log.i("ProfileRootScreen", "Нажата кнопка: Черный список")
            //     }
            // )

            // Spacer прижимает кнопку выхода к низу экрана
            Spacer(modifier = Modifier.weight(1f))

            // Кнопка "Выйти"
            LogoutButton(
                onClick = {
                    showLogoutDialog = true
                }
            )
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
                                appContainer?.logoutUseCase?.invoke()
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
    modifier: Modifier = Modifier
) {
    SWButton(
        config = ButtonConfig(
            modifier = modifier.fillMaxWidth(),
            size = SWButtonSize.LARGE,
            mode = SWButtonMode.TINTED,
            text = stringResource(id = R.string.edit_profile),
            onClick = onClick
        )
    )
}

/**
 * Кнопка "Друзья"
 */
@Composable
private fun FriendsButton(
    friendsCount: Int,
    friendRequestsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.friends),
            trailingText = pluralStringResource(
                id = R.plurals.friendsCount,
                count = friendsCount,
                friendsCount
            ),
            badgeValue = if (friendRequestsCount > 0) friendRequestsCount else null,
            enabled = true
        )
    }
}

/**
 * Кнопка "Где тренируется"
 */
@Composable
private fun UsedParksButton(
    parksCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.where_trains),
            trailingText = pluralStringResource(
                id = R.plurals.parksCount,
                count = parksCount,
                parksCount
            ),
            enabled = true
        )
    }
}

/**
 * Кнопка "Добавленные площадки"
 */
@Composable
private fun AddedParksButton(
    addedParksCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.male_added_parks),
            trailingText = pluralStringResource(
                id = R.plurals.parksCount,
                count = addedParksCount,
                addedParksCount
            ),
            enabled = true
        )
    }
}

/**
 * Кнопка "Дневники"
 */
@Composable
private fun JournalsButton(
    journalsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.journals),
            trailingText = pluralStringResource(
                id = R.plurals.journalsCount,
                count = journalsCount,
                journalsCount
            ),
            enabled = true
        )
    }
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
            onClickAuth = {}
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


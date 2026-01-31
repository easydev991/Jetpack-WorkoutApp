package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.AppContainer
import com.swparks.ui.ds.IncognitoProfileView
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
        // Авторизован - показываем профиль и кнопку выхода
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_regular),
                    end = dimensionResource(R.dimen.spacing_regular),
                    top = dimensionResource(R.dimen.spacing_regular),
                    bottom = dimensionResource(R.dimen.spacing_regular)
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    // Показываем заглушку, пока загружаются данные
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

            LogoutButton(
                onClick = {
                    scope.launch {
                        // Вызываем usecase напрямую
                        appContainer?.logoutUseCase?.invoke()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.profile))
        },
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


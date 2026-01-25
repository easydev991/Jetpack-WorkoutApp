package com.swparks.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.swparks.R
import com.swparks.model.User
import com.swparks.navigation.Screen
import com.swparks.ui.ds.IncognitoProfileView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.UserProfileCardView
import com.swparks.ui.ds.UserProfileData
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRootScreen(
    modifier: Modifier = Modifier,
    user: User?,
    appContainer: com.swparks.data.DefaultAppContainer? = null,
    navController: NavController? = null,
    isLoggingOut: Boolean = false,
    onLogout: () -> Unit = {},
    onLogoutComplete: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.profile))
                },
            )
        }
    ) { paddingValues ->
        if (user == null) {
            // Не авторизован - показываем IncognitoProfileView
            IncognitoProfileView(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(
                        start = dimensionResource(R.dimen.spacing_regular),
                        end = dimensionResource(R.dimen.spacing_regular)
                    ),
                onClickAuth = {
                    navController?.navigate(Screen.Login.route)
                }
            )
        } else {
            // Авторизован - показываем профиль и кнопку выхода
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(
                        start = dimensionResource(R.dimen.spacing_regular),
                        end = dimensionResource(R.dimen.spacing_regular),
                        top = dimensionResource(R.dimen.spacing_regular),
                        bottom = dimensionResource(R.dimen.spacing_regular)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                UserProfileCardView(
                    data = UserProfileData(
                        modifier = Modifier,
                        imageStringURL = user.image,
                        userName = user.fullName ?: user.name,
                        gender = user.genderOption?.let { stringResource(id = it.description) }
                            ?: "",
                        age = remember { calculateAge(user.birthDate) },
                        shortAddress = "${getCountryName(user.countryID)}, ${getCityName(user.cityID)}"
                    )
                )

                LogoutButton(
                    onClick = {
                        onLogout()
                    },
                    enabled = !isLoggingOut
                )

                // Выполняем logout при изменении флага
                androidx.compose.runtime.LaunchedEffect(isLoggingOut) {
                    if (isLoggingOut) {
                        appContainer?.logoutUseCase?.invoke()
                        onLogoutComplete()
                    }
                }
            }
        }
    }
}

/**
 * Вычисляет возраст из даты рождения
 *
 * @param birthDateString Дата рождения в формате "YYYY-MM-DD"
 * @return Возраст в годах, или 0 если дата отсутствует или неверна
 */
private fun calculateAge(birthDateString: String?): Int {
    if (birthDateString.isNullOrBlank()) {
        return 0
    }
    return try {
        val birthDate = LocalDate.parse(birthDateString)
        val currentDate = LocalDate.now()
        Period.between(birthDate, currentDate).years
    } catch (e: java.time.format.DateTimeParseException) {
        Log.e("ProfileRootScreen", "Ошибка при вычислении возраста: ${e.message}")
        0
    } catch (e: IllegalArgumentException) {
        Log.e("ProfileRootScreen", "Ошибка при вычислении возраста: ${e.message}")
        0
    }
}

/**
 * Возвращает название страны по ID
 */
private fun getCountryName(countryId: Int?): String {
    return if (countryId != null) "Страна #$countryId" else ""
}

/**
 * Возвращает название города по ID
 */
private fun getCityName(cityId: Int?): String {
    return if (cityId != null) "Город #$cityId" else ""
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
    SWButton(
        config = com.swparks.ui.ds.ButtonConfig(
            modifier = modifier.fillMaxWidth(),
            size = SWButtonSize.LARGE,
            mode = SWButtonMode.TINTED,
            text = stringResource(id = R.string.logout),
            enabled = enabled,
            onClick = onClick
        )
    )
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ProfileRootScreenPreview() {
    JetpackWorkoutAppTheme {
        ProfileRootScreen(user = null)
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ProfileRootScreenLoggedInPreview() {
    val testUser = User(
        id = 1,
        name = "testuser",
        image = "",
        cityID = null,
        countryID = null,
        birthDate = "1990-11-25",
        email = "test@example.com",
        fullName = "Test User",
        genderCode = 0,
        friendRequestCount = "0",
        friendsCount = 5,
        parksCount = "2",
        addedParks = null,
        journalCount = 1,
        lang = "ru"
    )
    JetpackWorkoutAppTheme {
        ProfileRootScreen(user = testUser)
    }
}


package com.swparks.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.model.Park
import com.swparks.model.SocialUpdates
import com.swparks.model.User
import com.swparks.navigation.AppState
import com.swparks.navigation.BottomNavigationBar
import com.swparks.navigation.Screen
import com.swparks.ui.screens.auth.LoginScreen
import com.swparks.ui.screens.events.EventsScreen
import com.swparks.ui.screens.messages.MessagesRootScreen
import com.swparks.ui.screens.more.MoreScreen
import com.swparks.ui.screens.parks.ParksRootScreen
import com.swparks.ui.screens.profile.ProfileRootScreen
import com.swparks.ui.screens.themeicon.ThemeIconScreen
import com.swparks.utils.ReadJSONFromAssets
import com.swparks.utils.WorkoutAppJson
import com.swparks.viewmodel.ThemeIconViewModel

@Composable
fun RootScreen(appState: AppState) {
    // Создаем AppContainer для доступа к use cases
    val context = LocalContext.current
    val appContainer = remember {
        com.swparks.data.DefaultAppContainer(context.applicationContext)
    }

    // Состояние для хранения авторизованного пользователя
    val currentUser = remember { androidx.compose.runtime.mutableStateOf<User?>(null) }
    val isLoggingOut = remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(appState = appState)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        NavHost(
            navController = appState.navController,
            startDestination = Screen.Parks.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            // Вкладка "Площадки"
            composable(route = Screen.Parks.route) {
                val context = LocalContext.current
                val parks = remember {
                    val oldParks = ReadJSONFromAssets(context, "parks.json")
                    WorkoutAppJson.decodeFromString<List<Park>>(oldParks)
                }
                ParksRootScreen(
                    parks = parks,
                )
            }

            // Вкладка "Мероприятия"
            composable(route = Screen.Events.route) {
                EventsScreen()
            }

            // Вкладка "Сообщения"
            composable(route = Screen.Messages.route) {
                MessagesRootScreen()
            }

            // Вкладка "Профиль"
            composable(route = Screen.Profile.route) {
                ProfileRootScreen(
                    user = currentUser.value,
                    appContainer = appContainer,
                    navController = appState.navController,
                    isLoggingOut = isLoggingOut.value,
                    onLogout = {
                        isLoggingOut.value = true
                    },
                    onLogoutComplete = {
                        currentUser.value = null
                        isLoggingOut.value = false
                    }
                )
            }

            // Вкладка "Ещё"
            composable(route = Screen.More.route) {
                MoreScreen(navController = appState.navController)
            }

            // Детальные экраны площадок (будут добавлены позже)
            composable(route = Screen.ParkDetail.route) {
                // TODO: Реализовать ParkDetailScreen
            }

            composable(route = Screen.CreatePark.route) {
                // TODO: Реализовать CreateParkScreen
            }

            composable(route = Screen.EditPark.route) {
                // TODO: Реализовать EditParkScreen
            }

            // Детальные экраны мероприятий (будут добавлены позже)
            composable(route = Screen.EventDetail.route) {
                // TODO: Реализовать EventDetailScreen
            }

            composable(route = Screen.CreateEvent.route) {
                // TODO: Реализовать CreateEventScreen
            }

            composable(route = Screen.EditEvent.route) {
                // TODO: Реализовать EditEventScreen
            }

            // Экраны сообщений (будут добавлены позже)
            composable(route = Screen.Chat.route) {
                // TODO: Реализовать ChatScreen
            }

            composable(route = Screen.Friends.route) {
                // TODO: Реализовать FriendsScreen
            }

            composable(route = Screen.UserSearch.route) {
                // TODO: Реализовать UserSearchScreen
            }

            // Экраны профиля (будут добавлены позже)
            composable(route = Screen.EditProfile.route) {
                // TODO: Реализовать EditProfileScreen
            }

            composable(route = Screen.UserParks.route) {
                // TODO: Реализовать UserParksScreen
            }

            composable(route = Screen.UserTrainingParks.route) {
                // TODO: Реализовать UserTrainingParksScreen
            }

            composable(route = Screen.Blacklist.route) {
                // TODO: Реализовать BlacklistScreen
            }

            composable(route = Screen.JournalsList.route) {
                // TODO: Реализовать JournalsListScreen
            }

            composable(route = Screen.JournalDetail.route) {
                // TODO: Реализовать JournalDetailScreen
            }

            composable(route = Screen.CreateJournal.route) {
                // TODO: Реализовать CreateJournalScreen
            }

            composable(route = Screen.EditJournal.route) {
                // TODO: Реализовать EditJournalScreen
            }

            composable(route = Screen.AddJournalEntry.route) {
                // TODO: Реализовать AddJournalEntryScreen
            }

            composable(route = Screen.ChangePassword.route) {
                // TODO: Реализовать ChangePasswordScreen
            }

            composable(route = Screen.SelectCountry.route) {
                // TODO: Реализовать SelectCountryScreen
            }

            composable(route = Screen.SelectCity.route) {
                // TODO: Реализовать SelectCityScreen
            }

            // Экраны настроек (будут добавлены позже)
            composable(route = Screen.ThemeIcon.route) {
                val context = LocalContext.current
                val appSettingsDataStore = remember { AppSettingsDataStore(context) }
                val factory = remember(appSettingsDataStore) {
                    ThemeIconViewModel.factory(
                        appSettingsDataStore,
                        context.applicationContext as android.app.Application
                    )
                }
                val viewModel = androidx.lifecycle.ViewModelProvider(
                    androidx.lifecycle.ViewModelStore(),
                    factory
                )[ThemeIconViewModel::class.java]
                ThemeIconScreen(
                    viewModel = viewModel,
                    onBackClick = { appState.navController.popBackStack() }
                )
            }

            // Экраны авторизации (модальные окна)
            composable(route = Screen.Login.route) {
                LoginScreen(
                    onDismiss = {
                        appState.navController.popBackStack()
                    },
                    onLoginSuccess = { result: Result<SocialUpdates> ->
                        result.onSuccess { socialUpdates ->
                            // Обновляем состояние с данными пользователя
                            currentUser.value = socialUpdates.user
                            // Закрываем LoginScreen
                            appState.navController.popBackStack()
                        }
                        result.onFailure { error ->
                            // Ошибка загрузки данных пользователя
                            // LoginScreen остается открытым
                        }
                    }
                )
            }

            composable(route = Screen.Register.route) {
                // TODO: Реализовать RegisterScreen как модальное окно
            }
        }
    }
}

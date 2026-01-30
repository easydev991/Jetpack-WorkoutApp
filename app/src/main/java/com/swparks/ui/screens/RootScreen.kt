package com.swparks.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.model.Park
import com.swparks.navigation.AppState
import com.swparks.navigation.BottomNavigationBar
import com.swparks.navigation.Screen
import com.swparks.ui.screens.auth.LoginSheetHost
import com.swparks.ui.screens.events.EventsScreen
import com.swparks.ui.screens.events.EventsTopAppBar
import com.swparks.ui.screens.messages.MessagesRootScreen
import com.swparks.ui.screens.messages.MessagesTopAppBar
import com.swparks.ui.screens.more.MoreScreen
import com.swparks.ui.screens.more.MoreTopAppBar
import com.swparks.ui.screens.parks.ParksRootScreen
import com.swparks.ui.screens.parks.ParksTopAppBar
import com.swparks.ui.screens.profile.ProfileRootScreen
import com.swparks.ui.screens.profile.ProfileTopAppBar
import com.swparks.ui.screens.themeicon.ThemeIconScreen
import com.swparks.utils.ReadJSONFromAssets
import com.swparks.utils.WorkoutAppJson
import com.swparks.viewmodel.ThemeIconViewModel

@Composable
fun RootScreen(appState: AppState) {
    // Используем единый AppContainer из Application — иначе ProfileViewModel и LoginViewModel
    // работают с разными экземплярами БД/DataStore, и после авторизации UI профиля не обновляется
    val context = LocalContext.current
    val appContainer = remember {
        (context.applicationContext as JetpackWorkoutApplication).container
    }

    val isLoggingOut = remember { mutableStateOf(false) }
    // Состояние для LoginSheet
    var showLoginSheet by remember { mutableStateOf(false) }

    // Создаем ProfileViewModel ЕДИН РАЗ на уровне RootScreen
    // Это предотвращает пересоздание ViewModel при навигации между вкладками
    // и гарантирует, что ProfileViewModel всегда подписан на currentUser Flow
    val profileViewModel = remember {
        appContainer.profileViewModelFactory()
    }

    // Загружаем parks для использования в TopBar и в ParksRootScreen
    val parks = remember {
        val oldParks = ReadJSONFromAssets(context, "parks.json")
        WorkoutAppJson.decodeFromString<List<Park>>(oldParks)
    }

    Scaffold(
        topBar = {
            when (appState.currentTopLevelDestination?.route) {
                Screen.Parks.route -> {
                    ParksTopAppBar(
                        parksCount = parks.size
                    )
                }

                Screen.Events.route -> {
                    EventsTopAppBar()
                }

                Screen.Messages.route -> {
                    MessagesTopAppBar()
                }

                Screen.Profile.route -> {
                    ProfileTopAppBar()
                }

                Screen.More.route -> {
                    MoreTopAppBar()
                }

                null -> {}
                else -> {}
            }
        },
        bottomBar = {
            BottomNavigationBar(appState = appState)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        NavHost(
            navController = appState.navController,
            startDestination = Screen.Parks.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Вкладка "Площадки"
            composable(route = Screen.Parks.route) {
                ParksRootScreen(
                    parks = parks,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Вкладка "Мероприятия"
            composable(route = Screen.Events.route) {
                EventsScreen(
                    contentModifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Вкладка "Сообщения"
            composable(route = Screen.Messages.route) {
                MessagesRootScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Вкладка "Профиль"
            composable(route = Screen.Profile.route) {
                // Используем единый ProfileViewModel, созданный на уровне RootScreen
                ProfileRootScreen(
                    appContainer = appContainer,
                    viewModel = profileViewModel,
                    isLoggingOut = isLoggingOut.value,
                    onLogout = {
                        isLoggingOut.value = true
                    },
                    onLogoutComplete = {
                        isLoggingOut.value = false
                    },
                    onShowLoginSheet = { showLoginSheet = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Вкладка "Ещё"
            composable(route = Screen.More.route) {
                MoreScreen(
                    navController = appState.navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
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
                // LoginScreen теперь показывается через LoginSheetHost
            }

            composable(route = Screen.Register.route) {
                // TODO: Реализовать RegisterScreen как модальное окно
            }
        }

        // LoginSheetHost поверх NavHost
        LoginSheetHost(
            show = showLoginSheet,
            onDismissed = { showLoginSheet = false },
            onLoginSuccess = { userId ->
                // Успешная авторизация - загружаем профиль с сервера
                profileViewModel.loadUserProfileFromServer(userId)
                // Закрываем LoginSheet
                showLoginSheet = false
                // Навигируем на вкладку профиля, чтобы гарантированно обновить UI
                appState.navigateToProfile()
            }
        )
    }
}

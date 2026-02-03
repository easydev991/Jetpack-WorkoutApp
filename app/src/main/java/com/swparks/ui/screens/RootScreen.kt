package com.swparks.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.swparks.ui.screens.parks.ParksAddedByUserScreen
import com.swparks.ui.screens.parks.ParksRootScreen
import com.swparks.ui.screens.parks.ParksTopAppBar
import com.swparks.ui.screens.profile.MyFriendsScreen
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

    // Состояние для LoginSheet
    var showLoginSheet by remember { mutableStateOf(false) }

    // Создаем ProfileViewModel ЕДИН РАЗ на уровне RootScreen
    // Это предотвращает пересоздание ViewModel при навигации между вкладками
    // и гарантирует, что ProfileViewModel всегда подписан на currentUser Flow
    val profileViewModel = remember {
        appContainer.profileViewModelFactory()
    }

    // Подписываемся на Flow из ProfileViewModel для реактивного обновления
    val currentUser by profileViewModel.currentUser.collectAsState()

    // Синхронизируем currentUser с AppState при каждом изменении
    LaunchedEffect(currentUser) {
        appState.updateCurrentUser(currentUser)
    }

    // Загружаем parks для использования в TopBar и в ParksRootScreen
    val parks = remember {
        val oldParks = ReadJSONFromAssets(context, "parks.json")
        WorkoutAppJson.decodeFromString<List<Park>>(oldParks)
    }

    Scaffold(
        topBar = {
            // Показываем TopAppBar только для корневых экранов вкладок
            // Для дочерних экранов (parentTab != null) TopAppBar показывается внутри самого экрана
            if (appState.isCurrentRouteTopLevel) {
                when (appState.currentTopLevelDestination?.route) {
                    Screen.Parks.route -> {
                        ParksTopAppBar(
                            appState = appState,
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
                        ProfileTopAppBar(
                            appState = appState,
                            onSearchUsersClick = {
                                Log.i("RootScreen", "Нажата кнопка: Поиск пользователей")
                            }
                        )
                    }

                    Screen.More.route -> {
                        MoreTopAppBar()
                    }

                    null -> {}
                    else -> {}
                }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    parks = parks,
                    onParkClick = {
                        Log.d("ParksRootScreen", "Нажата площадка: ${it.name}")
                    }
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
                    appState = appState,
                    onShowLoginSheet = { showLoginSheet = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Вкладка "Ещё"
            composable(route = Screen.More.route) {
                MoreScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    navController = appState.navController
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
                val currentUser by profileViewModel.currentUser.collectAsState()
                val addedParks = currentUser?.addedParks ?: emptyList()
                ParksAddedByUserScreen(
                    modifier = Modifier.fillMaxSize(),
                    parks = addedParks,
                    onBackClick = { appState.navController.popBackStack() },
                    onParkClick = { park ->
                        Log.d("RootScreen", "Нажата площадка: ${park.name}")
                    },
                    parentPaddingValues = paddingValues
                )
            }

            composable(route = Screen.MyFriends.route) {
                val viewModel = remember(appContainer) {
                    appContainer.friendsListViewModelFactory()
                }
                MyFriendsScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    onBackClick = { appState.navController.popBackStack() },
                    parentPaddingValues = paddingValues
                )
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

            // Экраны настроек
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
                    onBackClick = { appState.navController.popBackStack() },
                    parentPaddingValues = paddingValues
                )
            }

        }

        // LoginSheetHost поверх NavHost
        LoginSheetHost(
            show = showLoginSheet,
            onDismissed = { showLoginSheet = false },
            onLoginSuccess = { userId ->
                // Успешная авторизация - загружаем профиль с сервера
                profileViewModel.loadProfileFromServer(userId)
                // Закрываем LoginSheet
                showLoginSheet = false
                // Навигируем на вкладку профиля, чтобы гарантированно обновить UI
                appState.navigateToProfile()
            }
        )
    }
}

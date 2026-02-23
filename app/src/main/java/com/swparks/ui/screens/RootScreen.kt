package com.swparks.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.swparks.data.model.Park
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.navigation.AppState
import com.swparks.navigation.BottomNavigationBar
import com.swparks.navigation.Screen
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.screens.auth.LoginSheetHost
import com.swparks.ui.screens.auth.RegisterSheetHost
import com.swparks.ui.screens.events.EventsScreen
import com.swparks.ui.screens.events.EventsTopAppBar
import com.swparks.ui.screens.journals.JournalEntriesScreen
import com.swparks.ui.screens.journals.JournalsListScreen
import com.swparks.ui.screens.messages.MessagesRootScreen
import com.swparks.ui.screens.messages.MessagesTopAppBar
import com.swparks.ui.screens.more.MoreScreen
import com.swparks.ui.screens.more.MoreTopAppBar
import com.swparks.ui.screens.parks.ParksAddedByUserScreen
import com.swparks.ui.screens.parks.ParksRootScreen
import com.swparks.ui.screens.parks.ParksTopAppBar
import com.swparks.ui.screens.profile.ChangePasswordScreen
import com.swparks.ui.screens.profile.EditProfileScreen
import com.swparks.ui.screens.profile.MyBlacklistScreen
import com.swparks.ui.screens.profile.MyFriendsScreen
import com.swparks.ui.screens.profile.OtherUserProfileScreen
import com.swparks.ui.screens.profile.ProfileRootScreen
import com.swparks.ui.screens.profile.ProfileTopAppBar
import com.swparks.ui.screens.profile.SearchUserScreen
import com.swparks.ui.screens.profile.SelectCityScreen
import com.swparks.ui.screens.profile.SelectCountryScreen
import com.swparks.ui.screens.profile.UserFriendsScreen
import com.swparks.ui.screens.profile.UserTrainingParksScreen
import com.swparks.ui.screens.themeicon.ThemeIconScreen
import com.swparks.ui.viewmodel.ThemeIconViewModel
import com.swparks.util.toUiText
import com.swparks.utils.ReadJSONFromAssets
import com.swparks.utils.WorkoutAppJson

@Composable
fun RootScreen(appState: AppState) {
    // Используем единый AppContainer из Application — иначе ProfileViewModel и LoginViewModel
    // работают с разными экземплярами БД/DataStore, и после авторизации UI профиля не обновляется
    val context = LocalContext.current
    val appContainer = remember {
        (context.applicationContext as JetpackWorkoutApplication).container
    }

    // Состояние для Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Собираем ошибки из UserNotifier и показываем в Snackbar
    LaunchedEffect(Unit) {
        appContainer.userNotifier.errorFlow.collect { error ->
            // Логируем ошибку для отладки
            Log.e(
                "RootScreen",
                "Ошибка из UserNotifier: ${error.javaClass.simpleName}: ${error.message}"
            )

            // Преобразуем ошибку в локализованное сообщение для пользователя
            val message = error.toUiText(context)

            // Показываем Snackbar с кнопкой закрытия
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Собираем информационные уведомления и показываем в Snackbar
    LaunchedEffect(Unit) {
        appContainer.userNotifier.notificationFlow.collect { notification ->
            // Показываем Snackbar с сообщением
            snackbarHostState.showSnackbar(
                message = notification.message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Состояние для LoginSheet
    var showLoginSheet by remember { mutableStateOf(false) }

    // Состояние для RegisterSheet
    var showRegisterSheet by remember { mutableStateOf(false) }

    // Создаем ProfileViewModel ЕДИН РАЗ на уровне RootScreen
    // Это предотвращает пересоздание ViewModel при навигации между вкладками
    // и гарантирует, что ProfileViewModel всегда подписан на currentUser Flow
    val profileViewModel = remember {
        appContainer.profileViewModelFactory()
    }

    // Создаем DialogsViewModel для экрана сообщений
    val dialogsViewModel = remember {
        appContainer.dialogsViewModelFactory()
    }

    // Создаем EditProfileViewModel для экранов редактирования профиля
    // Shared между EditProfileScreen, SelectCountryScreen, SelectCityScreen
    val editProfileViewModel = remember {
        appContainer.editProfileViewModelFactory()
    }

    // Экран черного списка
    remember {
        appContainer.blacklistViewModelFactory()
    }

    // Подписываемся на Flow из ProfileViewModel для реактивного обновления
    val currentUser by profileViewModel.currentUser.collectAsState()

    // Синхронизируем currentUser с AppState при каждом изменении
    LaunchedEffect(currentUser) {
        Log.d(
            "RootScreen",
            "currentUser изменился: ${currentUser?.id}, isAuthorized=${currentUser != null}"
        )
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
                                appState.navController.navigate(Screen.UserSearch.createRoute("profile"))
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            // Вкладка "Сообщения"
            composable(route = Screen.Messages.route) {
                MessagesRootScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = dialogsViewModel,
                    appState = appState,
                    onShowLoginSheet = {
                        showLoginSheet = true
                    },
                    onShowRegisterSheet = {
                        showRegisterSheet = true
                    },
                    onNavigateToFriends = {
                        appState.navController.navigate(Screen.MyFriends.route)
                    },
                    onNavigateToSearchUsers = {
                        appState.navController.navigate(Screen.UserSearch.createRoute("messages"))
                    }
                )
            }

            // Вкладка "Профиль"
            composable(route = Screen.Profile.route) {
                // Используем единый ProfileViewModel, созданный на уровне RootScreen
                ProfileRootScreen(
                    appContainer = appContainer,
                    viewModel = profileViewModel,
                    appState = appState,
                    onShowLoginSheet = {
                        showLoginSheet = true
                    },
                    onShowRegisterSheet = {
                        showRegisterSheet = true
                    },
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
                // FIXME: Реализовать ParkDetailScreen
            }

            composable(route = Screen.CreatePark.route) {
                // FIXME: Реализовать CreateParkScreen
            }

            composable(route = Screen.EditPark.route) {
                // FIXME: Реализовать EditParkScreen
            }

            // Детальные экраны мероприятий (будут добавлены позже)
            composable(route = Screen.EventDetail.route) {
                // FIXME: Реализовать EventDetailScreen
            }

            composable(route = Screen.CreateEvent.route) {
                // FIXME: Реализовать CreateEventScreen
            }

            composable(route = Screen.EditEvent.route) {
                // FIXME: Реализовать EditEventScreen
            }

            // Экраны сообщений (будут добавлены позже)
            composable(route = Screen.Chat.route) {
                // FIXME: Реализовать ChatScreen
            }

            composable(
                route = Screen.UserSearch.route,
                arguments = listOf(
                    androidx.navigation.navArgument("source") {
                        defaultValue = "messages"
                    }
                )
            ) {
                val viewModel = remember(appContainer) {
                    appContainer.searchUserViewModelFactory()
                }
                SearchUserScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    onBackClick = { appState.navController.popBackStack() },
                    onUserClick = { userId ->
                        appState.navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                    },
                    parentPaddingValues = paddingValues
                )
            }

            // Экраны профиля (будут добавлены позже)
            composable(route = Screen.EditProfile.route) {
                EditProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    currentUser = currentUser,
                    viewModel = editProfileViewModel,
                    onBackClick = { appState.navController.popBackStack() },
                    onNavigateToChangePassword = { appState.navController.navigate(Screen.ChangePassword.route) },
                    onNavigateToSelectCountry = {
                        appState.navController.navigate(Screen.SelectCountry.route)
                    },
                    onNavigateToSelectCity = { _, _ ->
                        appState.navController.navigate(Screen.SelectCity.route)
                    },
                    onNavigateToLogin = {
                        // После удаления профиля закрываем EditProfileScreen
                        // и возвращаемся на ProfileRootScreen, который покажет IncognitoProfileView
                        appState.navController.popBackStack(Screen.Profile.route, inclusive = false)
                    }
                )
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
                    onFriendClick = { userId ->
                        appState.navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                    },
                    parentPaddingValues = paddingValues
                )
            }

            composable(
                route = Screen.UserTrainingParks.route
            ) { navBackStackEntry ->
                val userId = navBackStackEntry.arguments?.getString("userId")?.toLongOrNull()
                if (userId != null) {
                    val viewModel = remember(appContainer) {
                        appContainer.userTrainingParksViewModelFactory(userId)
                    }
                    UserTrainingParksScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        onBackClick = { appState.navController.popBackStack() },
                        parentPaddingValues = paddingValues
                    )
                }
            }

            composable(
                route = Screen.UserFriends.route
            ) { navBackStackEntry ->
                val userId = navBackStackEntry.arguments?.getString("userId")?.toLongOrNull()
                if (userId != null) {
                    val viewModel = remember(appContainer, userId) {
                        appContainer.userFriendsViewModelFactory(userId)
                    }
                    UserFriendsScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        onBackClick = { appState.navController.popBackStack() },
                        onUserClick = { clickedUserId ->
                            appState.navController.navigate(
                                Screen.OtherUserProfile.createRoute(clickedUserId)
                            )
                        },
                        parentPaddingValues = paddingValues
                    )
                }
            }

            composable(route = Screen.Blacklist.route) {
                val viewModel = remember(appContainer) {
                    appContainer.blacklistViewModelFactory()
                }
                MyBlacklistScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    onBackClick = { appState.navController.popBackStack() },
                    parentPaddingValues = paddingValues
                )
            }

            composable(route = Screen.OtherUserProfile.route) { navBackStackEntry ->
                val userId = navBackStackEntry.arguments?.getString("userId")?.toLongOrNull()
                if (userId != null) {
                    val viewModel = remember(appContainer, userId) {
                        appContainer.otherUserProfileViewModelFactory(userId)
                    }
                    OtherUserProfileScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        appState = appState,
                        onBack = { appState.navController.popBackStack() },
                        onNavigateToOwnProfile = {
                            appState.navController.navigate(Screen.Profile.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(route = Screen.JournalsList.route) { navBackStackEntry ->
                val userId = navBackStackEntry.arguments?.getString("userId")?.toLongOrNull()
                if (userId != null) {
                    val viewModel = remember(appContainer) {
                        appContainer.journalsViewModelFactory(userId)
                    }
                    JournalsListScreen(
                        modifier = Modifier.fillMaxSize(),
                        appState = appState,
                        userId = userId,
                        viewModel = viewModel,
                        onBackClick = { appState.navController.popBackStack() },
                        onJournalClick = { journalId, journalOwnerId, journalTitle, viewAccess, commentAccess ->
                            appState.navController.navigate(
                                Screen.JournalEntries.createRoute(
                                    journalId,
                                    journalOwnerId,
                                    journalTitle,
                                    viewAccess,
                                    commentAccess
                                )
                            )
                        },
                        parentPaddingValues = paddingValues
                    )
                }
            }

            composable(route = Screen.JournalEntries.route) { navBackStackEntry ->
                val journalId = navBackStackEntry.arguments?.getString("journalId")?.toLongOrNull()
                // Получаем journalOwnerId (владелец дневника) из query-параметра
                val journalOwnerId =
                    navBackStackEntry.arguments?.getString("userId")?.toLongOrNull()
                // Получаем journalTitle из query-параметра
                val journalTitle = navBackStackEntry.arguments?.getString("journalTitle")?.let {
                    android.net.Uri.decode(it)
                } ?: ""
                // Получаем viewAccess из query-параметра
                val viewAccess = navBackStackEntry.arguments?.getString("viewAccess")
                    ?: JournalAccess.NOBODY.name
                // Получаем commentAccess из query-параметра
                val commentAccess = navBackStackEntry.arguments?.getString("commentAccess")
                    ?: JournalAccess.NOBODY.name

                Log.i("RootScreen", "=== JournalEntries Route ===")
                Log.i("RootScreen", "journalId=$journalId")
                Log.i(
                    "RootScreen",
                    "journalOwnerId=$journalOwnerId (из query-параметра userId)"
                )
                Log.i("RootScreen", "journalTitle=$journalTitle")
                Log.i("RootScreen", "viewAccess=$viewAccess")
                Log.i("RootScreen", "commentAccess=$commentAccess")

                if (journalId != null && journalOwnerId != null) {
                    val viewModel = remember(navBackStackEntry, appContainer) {
                        appContainer.journalEntriesViewModelFactory(
                            journalOwnerId = journalOwnerId,
                            journalId = journalId,
                            savedStateHandle = navBackStackEntry.savedStateHandle
                        )
                    }
                    JournalEntriesScreen(
                        modifier = Modifier.fillMaxSize(),
                        journalId = journalId,
                        journalTitle = journalTitle,
                        journalOwnerId = journalOwnerId,
                        journalViewAccess = viewAccess,
                        journalCommentAccess = commentAccess,
                        viewModel = viewModel,
                        appState = appState,
                        onBackClick = { appState.navController.popBackStack() },
                        parentPaddingValues = paddingValues
                    )
                }
            }

            composable(route = Screen.ChangePassword.route) {
                val changePasswordViewModel = remember(appContainer) {
                    appContainer.changePasswordViewModelFactory()
                }
                ChangePasswordScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = changePasswordViewModel,
                    onBackClick = { appState.navController.popBackStack() }
                )
            }

            composable(route = Screen.SelectCountry.route) {
                SelectCountryScreen(
                    viewModel = editProfileViewModel,
                    onBackClick = { appState.navController.popBackStack() }
                )
            }

            composable(route = Screen.SelectCity.route) {
                SelectCityScreen(
                    viewModel = editProfileViewModel,
                    onBackClick = { appState.navController.popBackStack() }
                )
            }

            // Экраны настроек
            composable(route = Screen.ThemeIcon.route) {
                val context = LocalContext.current
                val appSettingsDataStore = remember { AppSettingsDataStore(context) }
                val factory = remember(appSettingsDataStore) {
                    ThemeIconViewModel.factory(
                        appSettingsDataStore,
                        context.applicationContext as Application
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
                // Закрываем LoginSheet
                showLoginSheet = false
                // Успешная авторизация - загружаем профиль с сервера
                profileViewModel.loadProfileFromServer(userId)
                // Загружаем диалоги
                dialogsViewModel.loadDialogsAfterAuth()
            }
        )

        // RegisterSheetHost поверх NavHost
        RegisterSheetHost(
            show = showRegisterSheet,
            appContainer = appContainer,
            onDismissed = { showRegisterSheet = false },
            onRegisterSuccess = { userId ->
                // Закрываем RegisterSheet
                showRegisterSheet = false
                // Успешная регистрация - загружаем профиль с сервера
                profileViewModel.loadProfileFromServer(userId)
                // Загружаем диалоги
                dialogsViewModel.loadDialogsAfterAuth()
            }
        )
    }
}

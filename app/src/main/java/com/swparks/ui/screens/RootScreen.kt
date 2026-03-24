@file:Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod", "ForbiddenComment")

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.swparks.JetpackWorkoutApplication
import com.swparks.data.model.Park
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.navigation.AppState
import com.swparks.navigation.BottomNavigationBar
import com.swparks.navigation.CreateParkNavArgsViewModel
import com.swparks.navigation.EditEventNavArgsViewModel
import com.swparks.navigation.EditParkNavArgsViewModel
import com.swparks.navigation.EventParticipantsNavArgsViewModel
import com.swparks.navigation.ParkTraineesNavArgsViewModel
import com.swparks.navigation.Screen
import com.swparks.navigation.consumeJournalEntriesArgs
import com.swparks.navigation.consumeSelectedParkResult
import com.swparks.navigation.consumeUserAddedParksArgs
import com.swparks.navigation.consumeUserIdSourceArgs
import com.swparks.navigation.navigateToCreatePark
import com.swparks.navigation.navigateToEditEvent
import com.swparks.navigation.navigateToEditPark
import com.swparks.navigation.navigateToEventParticipants
import com.swparks.navigation.navigateToParkTrainees
import com.swparks.navigation.setSelectedParkResult
import com.swparks.ui.common.appViewModel
import com.swparks.ui.model.EventFormMode
import com.swparks.ui.model.ParkFormMode
import com.swparks.ui.model.ParticipantsMode
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.screens.auth.LoginSheetHost
import com.swparks.ui.screens.auth.RegisterSheetHost
import com.swparks.ui.screens.common.ParticipantsAction
import com.swparks.ui.screens.common.ParticipantsConfig
import com.swparks.ui.screens.common.ParticipantsScreen
import com.swparks.ui.screens.common.TextEntrySheetHost
import com.swparks.ui.screens.events.EventDetailAction
import com.swparks.ui.screens.events.EventDetailScreen
import com.swparks.ui.screens.events.EventFormNavigationAction
import com.swparks.ui.screens.events.EventFormScreen
import com.swparks.ui.screens.events.EventsScreen
import com.swparks.ui.screens.events.EventsTopAppBar
import com.swparks.ui.screens.journals.JournalEntriesScreen
import com.swparks.ui.screens.journals.JournalParams
import com.swparks.ui.screens.journals.JournalsListAction
import com.swparks.ui.screens.journals.JournalsListScreen
import com.swparks.ui.screens.journals.JournalsScreenConfig
import com.swparks.ui.screens.journals.JournalsScreenParams
import com.swparks.ui.screens.messages.ChatAction
import com.swparks.ui.screens.messages.ChatScreen
import com.swparks.ui.screens.messages.ChatUserParams
import com.swparks.ui.screens.messages.FriendsPickerConfig
import com.swparks.ui.screens.messages.MessagesFriendsPickerScreen
import com.swparks.ui.screens.messages.MessagesNavigationAction
import com.swparks.ui.screens.messages.MessagesRootScreen
import com.swparks.ui.screens.messages.MessagesTopAppBar
import com.swparks.ui.screens.more.MoreScreen
import com.swparks.ui.screens.more.MoreTopAppBar
import com.swparks.ui.screens.parks.ParkDetailAction
import com.swparks.ui.screens.parks.ParkDetailScreen
import com.swparks.ui.screens.parks.ParkFormNavigationAction
import com.swparks.ui.screens.parks.ParkFormScreen
import com.swparks.ui.screens.parks.ParksAddedByUserConfig
import com.swparks.ui.screens.parks.ParksAddedByUserScreen
import com.swparks.ui.screens.parks.ParksFilterDialog
import com.swparks.ui.screens.parks.ParksRootScreen
import com.swparks.ui.screens.parks.ParksTopAppBar
import com.swparks.ui.screens.profile.ChangePasswordScreen
import com.swparks.ui.screens.profile.EditProfileNavigationAction
import com.swparks.ui.screens.profile.EditProfileScreen
import com.swparks.ui.screens.profile.FriendAction
import com.swparks.ui.screens.profile.FriendsScreenConfig
import com.swparks.ui.screens.profile.MyBlacklistScreen
import com.swparks.ui.screens.profile.MyFriendsScreen
import com.swparks.ui.screens.profile.OtherUserProfileScreen
import com.swparks.ui.screens.profile.ProfileAuthAction
import com.swparks.ui.screens.profile.ProfileNavigationAction
import com.swparks.ui.screens.profile.ProfileRootConfig
import com.swparks.ui.screens.profile.ProfileRootScreen
import com.swparks.ui.screens.profile.ProfileTopAppBar
import com.swparks.ui.screens.profile.SearchUserAction
import com.swparks.ui.screens.profile.SearchUserConfig
import com.swparks.ui.screens.profile.SearchUserScreen
import com.swparks.ui.screens.profile.SelectCityScreen
import com.swparks.ui.screens.profile.SelectCountryScreen
import com.swparks.ui.screens.profile.UserFriendsAction
import com.swparks.ui.screens.profile.UserFriendsScreen
import com.swparks.ui.screens.profile.UserTrainingParksScreen
import com.swparks.ui.screens.themeicon.ThemeIconScreen
import com.swparks.ui.viewmodel.BlacklistViewModel
import com.swparks.ui.viewmodel.ChangePasswordViewModel
import com.swparks.ui.viewmodel.ChatViewModel
import com.swparks.ui.viewmodel.EventDetailViewModel
import com.swparks.ui.viewmodel.EventFormViewModel
import com.swparks.ui.viewmodel.EventsViewModel
import com.swparks.ui.viewmodel.FriendsListViewModel
import com.swparks.ui.viewmodel.JournalEntriesViewModel
import com.swparks.ui.viewmodel.JournalsViewModel
import com.swparks.ui.viewmodel.OtherUserProfileViewModel
import com.swparks.ui.viewmodel.ParkDetailViewModel
import com.swparks.ui.viewmodel.ParkFormViewModel
import com.swparks.ui.viewmodel.ParksRootViewModel
import com.swparks.ui.viewmodel.SearchUserViewModel
import com.swparks.ui.viewmodel.ThemeIconViewModel
import com.swparks.ui.viewmodel.UserAddedParksViewModel
import com.swparks.ui.viewmodel.UserFriendsViewModel
import com.swparks.ui.viewmodel.UserTrainingParksViewModel
import com.swparks.util.WorkoutAppJson
import com.swparks.util.readJSONFromAssets
import com.swparks.util.toUiText

private val BOTTOM_BAR_HIDDEN_BASE_ROUTES = setOf(
    Screen.CreateEvent,
    Screen.EditEvent,
    Screen.CreateEventForPark,
    Screen.SelectParkForEvent,
    Screen.EditProfile,
    Screen.ChangePassword,
    Screen.FriendsForDialog,
    Screen.CreatePark,
    Screen.EditPark,
    Screen.Chat
).map { it.route.substringBefore("/").substringBefore("?") }.toSet()

internal fun shouldShowBottomBar(route: String?): Boolean {
    val baseRoute = route
        ?.substringBefore("?")
        ?.substringBefore("/")
        .orEmpty()

    if (baseRoute.isBlank()) return true
    return baseRoute !in BOTTOM_BAR_HIDDEN_BASE_ROUTES
}

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
    val profileViewModel = appViewModel {
        appContainer.profileViewModelFactory()
    }

    // Создаем DialogsViewModel для экрана сообщений
    val dialogsViewModel = appViewModel {
        appContainer.dialogsViewModelFactory()
    }

    // Создаем EditProfileViewModel для экранов редактирования профиля
    // Shared между EditProfileScreen, SelectCountryScreen, SelectCityScreen
    val editProfileViewModel = appViewModel {
        appContainer.editProfileViewModelFactory()
    }

    // Создаем EventsViewModel на уровне RootScreen для возможности обновления
    // списка мероприятий при возврате с EventFormScreen после создания
    val eventsViewModel = viewModel<EventsViewModel>(factory = EventsViewModel.Factory)

    val parksRootViewModel = viewModel<ParksRootViewModel>(factory = ParksRootViewModel.Factory)

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
        val oldParks = readJSONFromAssets(context, "parks.json")
        WorkoutAppJson.decodeFromString<List<Park>>(oldParks)
    }

    val parksRootUiState by parksRootViewModel.uiState.collectAsState()
    val filteredParks by remember(parks, parksRootUiState.localFilter) {
        derivedStateOf { appContainer.filterParksUseCase(parks, parksRootUiState.localFilter) }
    }

    var isGettingLocationForCreatePark by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Показываем TopAppBar только для корневых экранов вкладок
            // Для дочерних экранов (parentTab != null) TopAppBar показывается внутри самого экрана
            if (appState.isCurrentRouteTopLevel) {
                when (appState.currentTopLevelDestination?.route) {
                    Screen.Parks.route -> {
                        ParksTopAppBar(
                            parksCount = filteredParks.size,
                            onFilterClick = { parksRootViewModel.onShowFilterDialog() },
                            isFilterLoading = parksRootUiState.isLoadingFilter
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
            val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: ""
            if (shouldShowBottomBar(currentRoute) && !isGettingLocationForCreatePark) {
                BottomNavigationBar(appState = appState)
            }
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
                    parks = filteredParks,
                    onParkClick = { park ->
                        appState.navController.navigate(Screen.ParkDetail.createRoute(park.id))
                    },
                    onCreateParkClick = { draft ->
                        Log.d("RootScreen", ">>> onCreateParkClick called with draft: $draft")
                        appState.navController.navigateToCreatePark(
                            source = "parks",
                            draft = draft
                        )
                        Log.d("RootScreen", "<<< navigateToCreatePark returned")
                    },
                    onGettingLocationStateChange = { isGettingLocationForCreatePark = it },
                    appState = appState,
                    viewModel = parksRootViewModel
                )
            }

            // Вкладка "Мероприятия"
            composable(route = Screen.Events.route) {
                EventsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = eventsViewModel,
                    onNavigateToEventDetail = { eventId ->
                        appState.navController.navigate(Screen.EventDetail.createRoute(eventId))
                    },
                    onNavigateToCreateEvent = {
                        appState.navController.navigate(Screen.CreateEvent.route)
                    }
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
                    onAction = { action ->
                        when (action) {
                            MessagesNavigationAction.ShowLoginSheet -> {
                                showLoginSheet = true
                            }

                            MessagesNavigationAction.ShowRegisterSheet -> {
                                showRegisterSheet = true
                            }

                            MessagesNavigationAction.NavigateToFriends -> {
                                appState.navController.navigate(Screen.FriendsForDialog.route)
                            }

                            MessagesNavigationAction.NavigateToSearchUsers -> {
                                appState.navController.navigate(Screen.UserSearch.createRoute("messages"))
                            }

                            is MessagesNavigationAction.NavigateToChat -> {
                                appState.navController.navigate(
                                    Screen.Chat.createRoute(
                                        action.dialogId,
                                        action.userId,
                                        action.userName,
                                        action.userImage,
                                        "messages"
                                    )
                                )
                            }
                        }
                    }
                )
            }

            // Вкладка "Профиль"
            composable(route = Screen.Profile.route) {
                ProfileRootScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = profileViewModel,
                    config = ProfileRootConfig(
                        appContainer = appContainer,
                        appState = appState
                    ),
                    onAuthAction = { action ->
                        when (action) {
                            ProfileAuthAction.ShowLoginSheet -> showLoginSheet = true
                            ProfileAuthAction.ShowRegisterSheet -> showRegisterSheet = true
                        }
                    }
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

            composable(
                route = Screen.ParkDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("parkId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "parks"
                    }
                )
            ) { navBackStackEntry ->
                val parkDetailSource = navBackStackEntry.arguments?.getString("source") ?: "parks"
                val parkDetailViewModel = viewModel<ParkDetailViewModel>(
                    factory = ParkDetailViewModel.factory(
                        swRepository = appContainer.swRepository,
                        countriesRepository = appContainer.countriesRepository,
                        userPreferencesRepository = appContainer.userPreferencesRepository,
                        userNotifier = appContainer.userNotifier,
                        logger = appContainer.logger
                    )
                )

                val updatedParkJson = navBackStackEntry.savedStateHandle
                    .getStateFlow<String?>("updatedPark", null)
                    .collectAsState()
                    .value

                LaunchedEffect(updatedParkJson) {
                    if (updatedParkJson != null) {
                        val updatedPark =
                            WorkoutAppJson.decodeFromString<Park>(
                                updatedParkJson
                            )
                        parkDetailViewModel.onParkUpdated(updatedPark.id)
                        navBackStackEntry.savedStateHandle.remove<String>("updatedPark")
                    }
                }

                ParkDetailScreen(
                    viewModel = parkDetailViewModel,
                    source = parkDetailSource,
                    parentPaddingValues = paddingValues,
                    onAction = { action ->
                        when (action) {
                            is ParkDetailAction.OnBack -> appState.navController.popBackStack()
                            is ParkDetailAction.OnParkDeleted -> {
                                Log.d("RootScreen", "Площадка удалена: ${action.parkId}")
                                appState.navController.getBackStackEntry(Screen.UserParks.route)
                                    .savedStateHandle["deletedParkId"] = action.parkId
                                appState.navController.popBackStack()
                            }

                            is ParkDetailAction.OnNavigateToUserProfile -> {
                                appState.navController.navigate(
                                    Screen.OtherUserProfile.createRoute(
                                        action.userId,
                                        parkDetailSource
                                    )
                                )
                            }

                            is ParkDetailAction.OnNavigateToTrainees -> {
                                appState.navController.navigateToParkTrainees(
                                    parkId = action.parkId,
                                    source = parkDetailSource,
                                    users = action.users
                                )
                            }

                            is ParkDetailAction.OnNavigateToCreateEvent -> {
                                appState.navController.navigate(
                                    Screen.CreateEventForPark.createRoute(
                                        parkId = action.parkId,
                                        parkName = action.parkName,
                                        source = parkDetailSource
                                    )
                                )
                            }

                            is ParkDetailAction.OnNavigateToEditPark -> {
                                appState.navController.navigateToEditPark(
                                    parkId = action.park.id,
                                    source = parkDetailSource,
                                    park = action.park
                                )
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.CreatePark.route,
                arguments = listOf(
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "parks"
                    }
                )
            ) { navBackStackEntry ->
                val createParkArgsViewModel: CreateParkNavArgsViewModel = viewModel(
                    factory = CreateParkNavArgsViewModel.factory(navBackStackEntry)
                )
                val createParkArgs = createParkArgsViewModel.args
                val draft = createParkArgs.draft

                val viewModel: ParkFormViewModel = viewModel(
                    factory = ParkFormViewModel.factory(
                        ParkFormMode.Create(
                            initialAddress = draft?.address ?: "",
                            initialLatitude = draft?.latitude?.toString() ?: "",
                            initialLongitude = draft?.longitude?.toString() ?: "",
                            initialCityId = draft?.cityId
                        ),
                        appContainer
                    )
                )

                ParkFormScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = viewModel,
                    onAction = { action ->
                        when (action) {
                            is ParkFormNavigationAction.Back -> appState.navController.popBackStack()
                            is ParkFormNavigationAction.BackWithSavedPark -> {
                                appState.navController.popBackStack()
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.EditPark.route,
                arguments = listOf(
                    androidx.navigation.navArgument("parkId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "parks"
                    }
                )
            ) { navBackStackEntry ->
                val editParkArgsViewModel: EditParkNavArgsViewModel = viewModel(
                    factory = EditParkNavArgsViewModel.factory(navBackStackEntry)
                )
                val editParkArgs = editParkArgsViewModel.args
                val park = editParkArgs?.park

                if (park != null) {
                    val viewModel: ParkFormViewModel = viewModel(
                        factory = ParkFormViewModel.factory(
                            ParkFormMode.Edit(parkId = editParkArgs.parkId, park = park),
                            appContainer
                        )
                    )

                    ParkFormScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        viewModel = viewModel,
                        onAction = { action ->
                            when (action) {
                                is ParkFormNavigationAction.Back -> appState.navController.popBackStack()
                                is ParkFormNavigationAction.BackWithSavedPark -> {
                                    val updatedParkJson = WorkoutAppJson.encodeToString(action.park)
                                    appState.navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("updatedPark", updatedParkJson)
                                    appState.navController.popBackStack()
                                }
                            }
                        }
                    )
                } else {
                    appState.navController.popBackStack()
                }
            }

            composable(
                route = Screen.ParkTrainees.route,
                arguments = listOf(
                    androidx.navigation.navArgument("parkId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "parks"
                    }
                )
            ) { navBackStackEntry ->
                val parkTraineesArgsViewModel: ParkTraineesNavArgsViewModel = viewModel(
                    factory = ParkTraineesNavArgsViewModel.factory(navBackStackEntry)
                )
                val parkTraineesArgs = parkTraineesArgsViewModel.args

                if (parkTraineesArgs != null) {
                    ParticipantsScreen(
                        config = ParticipantsConfig(
                            mode = ParticipantsMode.Park,
                            users = parkTraineesArgs.users,
                            currentUserId = currentUser?.id
                        ),
                        onAction = { action ->
                            when (action) {
                                ParticipantsAction.Back -> appState.navController.popBackStack()
                                is ParticipantsAction.UserClick -> appState.navController.navigate(
                                    Screen.OtherUserProfile.createRoute(
                                        action.userId,
                                        parkTraineesArgs.source
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // Детальные экраны мероприятий
            composable(
                route = Screen.EventDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("eventId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "events"
                    }
                )
            ) { navBackStackEntry ->
                val eventDetailViewModel = viewModel<EventDetailViewModel>(
                    factory = EventDetailViewModel.factory(
                        swRepository = appContainer.swRepository,
                        countriesRepository = appContainer.countriesRepository,
                        userPreferencesRepository = appContainer.userPreferencesRepository,
                        userNotifier = appContainer.userNotifier,
                        logger = appContainer.logger
                    )
                )

                val updatedEventJson = navBackStackEntry.savedStateHandle
                    .getStateFlow<String?>("updatedEvent", null)
                    .collectAsState()
                    .value

                LaunchedEffect(updatedEventJson) {
                    if (updatedEventJson != null) {
                        val updatedEvent =
                            WorkoutAppJson.decodeFromString<com.swparks.data.model.Event>(
                                updatedEventJson
                            )
                        eventDetailViewModel.onEventUpdated(updatedEvent)
                        eventsViewModel.onEventUpdated(updatedEvent)
                        navBackStackEntry.savedStateHandle.remove<String>("updatedEvent")
                    }
                }

                EventDetailScreen(
                    viewModel = eventDetailViewModel,
                    parentPaddingValues = paddingValues,
                    onAction = { action ->
                        when (action) {
                            is EventDetailAction.OnBack -> appState.navController.popBackStack()
                            is EventDetailAction.OnEventDeleted -> {
                                eventsViewModel.removeDeletedEvent(action.eventId)
                                appState.navController.popBackStack()
                            }

                            is EventDetailAction.OnNavigateToUserProfile -> {
                                appState.navController.navigate(
                                    Screen.OtherUserProfile.createRoute(action.userId, "events")
                                )
                            }

                            is EventDetailAction.OnNavigateToParticipants -> {
                                appState.navController.navigateToEventParticipants(
                                    eventId = action.eventId,
                                    source = "events",
                                    users = action.users
                                )
                            }

                            is EventDetailAction.OnNavigateToEditEvent -> {
                                appState.navController.navigateToEditEvent(
                                    eventId = action.event.id,
                                    source = "events",
                                    event = action.event
                                )
                            }
                        }
                    }
                )
            }

            composable(route = Screen.CreateEvent.route) { navBackStackEntry ->
                val viewModel: EventFormViewModel = viewModel(
                    factory = EventFormViewModel.factory(EventFormMode.RegularCreate, appContainer)
                )

                val selectedParkResult = navBackStackEntry.consumeSelectedParkResult()
                if (selectedParkResult != null) {
                    viewModel.onParkSelected(selectedParkResult.parkId, selectedParkResult.parkName)
                }

                EventFormScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = viewModel,
                    onAction = { action ->
                        when (action) {
                            is EventFormNavigationAction.Back -> appState.navController.popBackStack()
                            is EventFormNavigationAction.BackWithCreatedEvent -> {
                                eventsViewModel.addCreatedEvent(action.event)
                                appState.navController.popBackStack()
                            }

                            is EventFormNavigationAction.NavigateToSelectPark -> {
                                val userId = currentUser?.id ?: return@EventFormScreen
                                appState.navController.navigate(
                                    Screen.SelectParkForEvent.createRoute(userId, "events")
                                )
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.EditEvent.route,
                arguments = listOf(
                    androidx.navigation.navArgument("eventId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "events"
                    }
                )
            ) { navBackStackEntry ->
                val editEventArgsViewModel: EditEventNavArgsViewModel = viewModel(
                    factory = EditEventNavArgsViewModel.factory(navBackStackEntry)
                )
                val editEventArgs = editEventArgsViewModel.args
                val event = editEventArgs?.event

                val mode = if (event != null) {
                    EventFormMode.EditExisting(
                        eventId = editEventArgs.eventId,
                        event = event
                    )
                } else {
                    EventFormMode.RegularCreate
                }

                val viewModel: EventFormViewModel = viewModel(
                    factory = EventFormViewModel.factory(mode, appContainer)
                )

                val selectedParkResult = navBackStackEntry.consumeSelectedParkResult()
                if (selectedParkResult != null) {
                    viewModel.onParkSelected(
                        selectedParkResult.parkId,
                        selectedParkResult.parkName
                    )
                }

                EventFormScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = viewModel,
                    onAction = { action ->
                        when (action) {
                            is EventFormNavigationAction.Back -> appState.navController.popBackStack()
                            is EventFormNavigationAction.BackWithCreatedEvent -> {
                                val updatedEventJson =
                                    WorkoutAppJson.encodeToString(action.event)
                                appState.navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("updatedEvent", updatedEventJson)
                                appState.navController.popBackStack()
                            }

                            is EventFormNavigationAction.NavigateToSelectPark -> {
                                val userId = currentUser?.id ?: return@EventFormScreen
                                appState.navController.navigate(
                                    Screen.SelectParkForEvent.createRoute(userId, "events")
                                )
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.CreateEventForPark.route,
                arguments = listOf(
                    androidx.navigation.navArgument("parkId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("parkName") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "parks"
                    }
                )
            ) { navBackStackEntry ->
                val parkId = navBackStackEntry.arguments?.getLong("parkId") ?: 0L
                val parkName = navBackStackEntry.arguments?.getString("parkName") ?: ""
                val viewModel: EventFormViewModel = viewModel(
                    factory = EventFormViewModel.factory(
                        EventFormMode.CreateForSelected(parkId = parkId, parkName = parkName),
                        appContainer
                    )
                )
                EventFormScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = viewModel,
                    onAction = { action ->
                        when (action) {
                            is EventFormNavigationAction.Back -> appState.navController.popBackStack()
                            is EventFormNavigationAction.BackWithCreatedEvent -> {
                                eventsViewModel.addCreatedEvent(action.event)
                                appState.navController.popBackStack()
                            }

                            is EventFormNavigationAction.NavigateToSelectPark -> {
                                // Игнорируем - в CreateForSelected парк уже выбран
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.SelectParkForEvent.route,
                arguments = listOf(
                    androidx.navigation.navArgument("userId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "events"
                    }
                )
            ) { navBackStackEntry ->
                val userId = navBackStackEntry.arguments?.getLong("userId") ?: 0L
                val viewModel: UserTrainingParksViewModel = appViewModel {
                    appContainer.userTrainingParksViewModelFactory(userId)
                }
                UserTrainingParksScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    onBackClick = { appState.navController.popBackStack() },
                    selectionMode = true,
                    onParkSelected = { parkId, parkName ->
                        appState.navController.previousBackStackEntry?.setSelectedParkResult(
                            parkId = parkId,
                            parkName = parkName
                        )
                        appState.navController.popBackStack()
                    },
                    parentPaddingValues = paddingValues
                )
            }

            composable(
                route = Screen.EventParticipants.route,
                arguments = listOf(
                    androidx.navigation.navArgument("eventId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("source") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "events"
                    }
                )
            ) { navBackStackEntry ->
                val participantsArgsViewModel: EventParticipantsNavArgsViewModel = viewModel(
                    factory = EventParticipantsNavArgsViewModel.factory(navBackStackEntry)
                )
                val participantsArgs = participantsArgsViewModel.args

                if (participantsArgs != null) {
                    ParticipantsScreen(
                        config = ParticipantsConfig(
                            mode = ParticipantsMode.Event,
                            users = participantsArgs.users,
                            currentUserId = currentUser?.id
                        ),
                        onAction = { action ->
                            when (action) {
                                ParticipantsAction.Back -> appState.navController.popBackStack()
                                is ParticipantsAction.UserClick -> appState.navController.navigate(
                                    Screen.OtherUserProfile.createRoute(
                                        action.userId,
                                        participantsArgs.source
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // Экран чата
            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    androidx.navigation.navArgument("dialogId") {
                        type = androidx.navigation.NavType.LongType
                    },
                    androidx.navigation.navArgument("userId") {
                        type = androidx.navigation.NavType.IntType
                    },
                    androidx.navigation.navArgument("userName") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("userImage") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    },
                    androidx.navigation.navArgument("source") {
                        defaultValue = "messages"
                    }
                )
            ) { navBackStackEntry ->
                val arguments = navBackStackEntry.arguments
                val dialogId = arguments?.getLong("dialogId") ?: 0L
                val userId = arguments?.getInt("userId") ?: 0
                val userName = arguments?.getString("userName") ?: ""
                val userImage = arguments?.getString("userImage")?.takeIf { it.isNotEmpty() }

                val chatViewModel: ChatViewModel = appViewModel {
                    appContainer.chatViewModelFactory()
                }

                ChatScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = chatViewModel,
                    userParams = ChatUserParams(
                        userId = userId,
                        userName = userName,
                        userImage = userImage
                    ),
                    currentUserId = currentUser?.id?.toInt(),
                    onAction = { action ->
                        when (action) {
                            ChatAction.Back -> appState.navController.popBackStack()
                            ChatAction.AvatarClick -> {
                                appState.navController.navigate(
                                    Screen.OtherUserProfile.createRoute(userId.toLong(), "messages")
                                )
                            }

                            ChatAction.MessageSent -> {
                                dialogsViewModel.refresh()
                            }
                        }
                    }
                )

                // Загружаем сообщения при первом отображении
                LaunchedEffect(dialogId) {
                    chatViewModel.loadMessages(dialogId)
                }
            }

            composable(
                route = Screen.UserSearch.route,
                arguments = listOf(
                    androidx.navigation.navArgument("source") {
                        defaultValue = "messages"
                    }
                )
            ) { navBackStackEntry ->
                val viewModel: SearchUserViewModel = appViewModel {
                    appContainer.searchUserViewModelFactory()
                }
                // Получаем source из аргументов навигации для передачи в OtherUserProfile
                val source = navBackStackEntry.arguments?.getString("source") ?: "messages"
                SearchUserScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    config = SearchUserConfig(
                        parentPaddingValues = paddingValues,
                        currentUserId = currentUser?.id
                    ),
                    onAction = { action ->
                        when (action) {
                            SearchUserAction.Back -> appState.navController.popBackStack()
                            is SearchUserAction.UserClick -> appState.navController.navigate(
                                Screen.OtherUserProfile.createRoute(action.userId, source)
                            )

                            SearchUserAction.Search -> viewModel.onSearch()
                            SearchUserAction.Retry -> viewModel.onSearch()
                        }
                    }
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
                    onAction = { action ->
                        when (action) {
                            EditProfileNavigationAction.Back -> appState.navController.popBackStack()
                            EditProfileNavigationAction.ChangePassword -> appState.navController.navigate(
                                Screen.ChangePassword.route
                            )

                            EditProfileNavigationAction.SelectCountry -> appState.navController.navigate(
                                Screen.SelectCountry.route
                            )

                            EditProfileNavigationAction.SelectCity -> appState.navController.navigate(
                                Screen.SelectCity.route
                            )

                            EditProfileNavigationAction.NavigateToLogin -> {
                                appState.navController.popBackStack(
                                    Screen.Profile.route,
                                    inclusive = false
                                )
                            }
                        }
                    }
                )
            }

            composable(route = Screen.UserParks.route) { navBackStackEntry ->
                val navArgs = navBackStackEntry.consumeUserAddedParksArgs()
                if (navArgs != null) {
                    val viewModel: UserAddedParksViewModel = appViewModel {
                        appContainer.userAddedParksViewModelFactory(
                            userId = navArgs.userId,
                            seedParks = navArgs.seedParks,
                            requiresFetch = navArgs.requiresFetch
                        )
                    }

                    ParksAddedByUserScreen(
                        config = ParksAddedByUserConfig(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel,
                            onBackClick = { appState.navController.popBackStack() },
                            onParkClick = { park: Park ->
                                appState.navController.navigate(
                                    Screen.ParkDetail.createRoute(
                                        park.id,
                                        "profile"
                                    )
                                )
                            },
                            parentPaddingValues = paddingValues,
                            navBackStackEntry = navBackStackEntry
                        )
                    )
                }
            }

            composable(route = Screen.MyFriends.route) {
                val viewModel: FriendsListViewModel = appViewModel {
                    appContainer.friendsListViewModelFactory()
                }
                MyFriendsScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    config = FriendsScreenConfig(
                        parentPaddingValues = paddingValues,
                        currentUserId = currentUser?.id
                    ),
                    onFriendClick = { userId ->
                        appState.navController.navigate(
                            Screen.OtherUserProfile.createRoute(userId, "profile")
                        )
                    },
                    onAction = { action ->
                        when (action) {
                            is FriendAction.Accept -> viewModel.onAcceptFriendRequest(action.userId)
                            is FriendAction.Decline -> viewModel.onDeclineFriendRequest(action.userId)
                            is FriendAction.Click -> appState.navController.popBackStack()
                        }
                    }
                )
            }

            composable(route = Screen.FriendsForDialog.route) {
                val viewModel: FriendsListViewModel = appViewModel {
                    appContainer.friendsListViewModelFactory()
                }

                var selectedFriend by remember { mutableStateOf<Pair<Long, String>?>(null) }
                var showTextEntrySheet by remember { mutableStateOf(false) }

                MessagesFriendsPickerScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    config = FriendsPickerConfig(
                        parentPaddingValues = paddingValues,
                        currentUserId = currentUser?.id
                    ),
                    onFriendClick = { userId, userName ->
                        selectedFriend = Pair(userId, userName)
                        showTextEntrySheet = true
                    },
                    onBackClick = {
                        appState.navController.popBackStack()
                    }
                )

                if (showTextEntrySheet && selectedFriend != null) {
                    TextEntrySheetHost(
                        show = true,
                        mode = TextEntryMode.Message(
                            userId = selectedFriend!!.first,
                            userName = selectedFriend!!.second
                        ),
                        onDismissed = {
                            showTextEntrySheet = false
                            selectedFriend = null
                        },
                        onSendSuccess = {
                            showTextEntrySheet = false
                            selectedFriend = null
                        }
                    )
                }
            }

            composable(
                route = Screen.UserTrainingParks.route
            ) { navBackStackEntry ->
                val args = navBackStackEntry.consumeUserIdSourceArgs()
                if (args != null) {
                    val viewModel: UserTrainingParksViewModel = appViewModel {
                        appContainer.userTrainingParksViewModelFactory(args.userId)
                    }
                    UserTrainingParksScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        onBackClick = { appState.navController.popBackStack() },
                        onParkClick = { park ->
                            appState.navController.navigate(
                                Screen.ParkDetail.createRoute(park.id, args.source)
                            )
                        },
                        parentPaddingValues = paddingValues
                    )
                }
            }

            composable(
                route = Screen.UserFriends.route
            ) { navBackStackEntry ->
                val args = navBackStackEntry.consumeUserIdSourceArgs()
                if (args != null) {
                    val viewModel: UserFriendsViewModel = appViewModel {
                        appContainer.userFriendsViewModelFactory(args.userId)
                    }
                    UserFriendsScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        config = FriendsScreenConfig(
                            parentPaddingValues = paddingValues,
                            currentUserId = currentUser?.id
                        ),
                        onAction = { action ->
                            when (action) {
                                UserFriendsAction.Back -> appState.navController.popBackStack()
                                is UserFriendsAction.UserClick -> appState.navController.navigate(
                                    Screen.OtherUserProfile.createRoute(action.userId, args.source)
                                )

                                UserFriendsAction.Refresh -> { /* handled internally */
                                }
                            }
                        }
                    )
                }
            }

            composable(route = Screen.Blacklist.route) {
                val viewModel: BlacklistViewModel = appViewModel {
                    appContainer.blacklistViewModelFactory()
                }
                MyBlacklistScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    onBackClick = { appState.navController.popBackStack() },
                    parentPaddingValues = paddingValues
                )
            }

            composable(
                route = Screen.OtherUserProfile.route,
                arguments = listOf(
                    androidx.navigation.navArgument("source") {
                        defaultValue = "profile"
                    }
                )
            ) { navBackStackEntry ->
                val args = navBackStackEntry.consumeUserIdSourceArgs()
                if (args != null) {
                    val viewModel: OtherUserProfileViewModel = appViewModel {
                        appContainer.otherUserProfileViewModelFactory(args.userId)
                    }
                    OtherUserProfileScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        appState = appState,
                        source = args.source,
                        onAction = { action ->
                            when (action) {
                                ProfileNavigationAction.Back -> appState.navController.popBackStack()
                                ProfileNavigationAction.NavigateToOwnProfile -> {
                                    appState.navController.navigate(Screen.Profile.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.JournalsList.route,
                arguments = listOf(
                    androidx.navigation.navArgument("source") {
                        defaultValue = "profile"
                    }
                )
            ) { navBackStackEntry ->
                val args = navBackStackEntry.consumeUserIdSourceArgs()
                if (args != null) {
                    val viewModel: JournalsViewModel = appViewModel {
                        appContainer.journalsViewModelFactory(args.userId)
                    }
                    JournalsListScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel,
                        config = JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(
                                userId = args.userId,
                                source = args.source
                            ),
                            parentPaddingValues = paddingValues
                        ),
                        onAction = { action ->
                            when (action) {
                                JournalsListAction.Back -> appState.navController.popBackStack()
                                is JournalsListAction.JournalClick -> {
                                    appState.navController.navigate(
                                        Screen.JournalEntries.createRoute(
                                            Screen.JournalEntries.JournalEntriesRoute(
                                                journalId = action.params.journalId,
                                                userId = action.params.journalOwnerId,
                                                journalTitle = action.params.journalTitle,
                                                viewAccess = action.params.viewAccess,
                                                commentAccess = action.params.commentAccess,
                                                source = args.source
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.JournalEntries.route,
                arguments = listOf(
                    androidx.navigation.navArgument("source") {
                        defaultValue = "profile"
                    }
                )
            ) { navBackStackEntry ->
                val args = navBackStackEntry.consumeJournalEntriesArgs()

                if (args != null) {
                    val viewModel: JournalEntriesViewModel = appViewModel {
                        appContainer.journalEntriesViewModelFactory(
                            journalOwnerId = args.journalOwnerId,
                            journalId = args.journalId,
                            savedStateHandle = navBackStackEntry.savedStateHandle
                        )
                    }
                    JournalEntriesScreen(
                        modifier = Modifier.fillMaxSize(),
                        params = JournalParams(
                            journalId = args.journalId,
                            journalTitle = args.journalTitle,
                            journalOwnerId = args.journalOwnerId,
                            journalViewAccess = args.viewAccess,
                            journalCommentAccess = args.commentAccess
                        ),
                        viewModel = viewModel,
                        appState = appState,
                        onBackClick = { appState.navController.popBackStack() },
                        parentPaddingValues = paddingValues
                    )
                }
            }

            composable(route = Screen.ChangePassword.route) {
                val changePasswordViewModel: ChangePasswordViewModel = appViewModel {
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
                // Сбрасываем transient visual state в BottomNavigation сразу после успеха логина.
                // Это закрывает короткое "окно" до обновления currentUser из ProfileViewModel.
                appState.bumpBottomNavVisualEpoch()
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
                // Аналогично логину: немедленный reset visual state нижней навигации.
                appState.bumpBottomNavVisualEpoch()
                // Успешная регистрация - загружаем профиль с сервера
                profileViewModel.loadProfileFromServer(userId)
                // Загружаем диалоги
                dialogsViewModel.loadDialogsAfterAuth()
            }
        )

        // ParksFilterDialog поверх NavHost
        if (parksRootUiState.showFilterDialog) {
            ParksFilterDialog(
                filter = parksRootUiState.localFilter,
                onFilterChange = { parksRootViewModel.onLocalFilterChange(it) },
                onApply = { parksRootViewModel.onFilterApply() },
                onDismiss = { parksRootViewModel.onDismissFilterDialog() }
            )
        }
    }
}

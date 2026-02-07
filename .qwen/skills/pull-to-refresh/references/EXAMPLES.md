# Примеры реализации pull-to-refresh

Все примеры основаны на реальном коде из проекта Jetpack-WorkoutApp.

## Полная реализация ProfileViewModel

```kotlin
package com.swparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.model.City
import com.swparks.model.Country
import com.swparks.model.User
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import com.swparks.util.setValueIfChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State для экрана профиля */
sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
        val country: Country? = null,
        val city: City? = null,
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}

/**
 * ViewModel для экрана профиля
 *
 * Управляет данными пользователя и справочником стран/городов
 *
 * @param countriesRepository Репозиторий для работы с данными стран и городов
 * @param swRepository Репозиторий для работы с данными пользователя и API
 * @param logger Логгер для записи сообщений
 * @param errorReporter Обработчик ошибок для отправки ошибок в UI
 */
class ProfileViewModel(
    private val countriesRepository: CountriesRepository,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
) : ViewModel() {

    private companion object {
        private const val STATE_TIMEOUT_MS = 5000L
        private const val TAG = "ProfileViewModel"
    }

    // Подписываемся на текущего пользователя из кэша
    val currentUser: StateFlow<User?> = swRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = null
        )

    // UI State
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Состояние обновления данных (pull-to-refresh)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser
                .collect { user ->
                    loadProfileAddress(user)
                }
        }
    }

    /**
     * Загружает профиль пользователя и социальные данные с сервера по userId.
     * Используется после успешной авторизации для загрузки свежих данных.
     *
     * @param userId ID пользователя для загрузки профиля и социальных данных
     */
    fun loadProfileFromServer(userId: Long) {
        viewModelScope.launch {
            try {
                logger.i(TAG, "Начало загрузки профиля с сервера: $userId")
                loadSocialUpdates(userId, updateUiState = true)
            } catch (e: Exception) {
                val errorMessage = "Ошибка загрузки профиля и социальных данных: ${e.message}"
                _uiState.update { ProfileUiState.Error(errorMessage) }
                logger.e(TAG, errorMessage)
            }
        }
    }

    /**
     * Обновляет данные профиля пользователя с сервера (для pull-to-refresh).
     * Использует текущего пользователя из репозитория.
     */
    fun refreshProfile() {
        val user = currentUser.value ?: run {
            logger.w(TAG, "Пропускаем обновление профиля: пользователь не авторизован")
            return
        }

        viewModelScope.launch {
            try {
                _isRefreshing.update { true }
                logger.i(TAG, "Начало обновления профиля: ${user.id}")

                loadSocialUpdates(user.id, updateUiState = false)
            } catch (e: Exception) {
                val errorMessage = "Ошибка обновления профиля: ${e.message}"
                logger.e(TAG, errorMessage)
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    /**
     * Загружает социальные данные пользователя с сервера.
     *
     * @param userId ID пользователя
     * @param updateUiState Если true, обновляет UI State при ошибке (для loadProfileFromServer)
     */
    private suspend fun loadSocialUpdates(userId: Long, updateUiState: Boolean) {
        swRepository.getSocialUpdates(userId)
            .onSuccess { socialUpdates ->
                // Данные сохранены в кэше через SWRepository.getSocialUpdates()
                // Теперь загружаем страну и город
                loadProfileAddress(socialUpdates.user)
                logger.i(TAG, "Профиль успешно загружен: ${socialUpdates.user.id}")
            }
            .onFailure { error ->
                val errorMessage = "Ошибка загрузки профиля и социальных данных: ${error.message}"
                if (updateUiState) {
                    _uiState.update { ProfileUiState.Error(errorMessage) }
                }
                logger.e(TAG, errorMessage)
            }
    }

    /**
     * Загружает данные профиля с информацией о стране и городе.
     * Используется когда пользователь уже загружен (например, при открытии экрана).
     *
     * @param user Пользователь для которого загружаем профиль
     */
    private fun loadProfileAddress(user: User?) {
        if (user == null) {
            _uiState.update { ProfileUiState.Error("Пользователь не авторизован") }
            logger.d(TAG, "Пропускаем загрузку адреса: пользователь null")
            return
        }

        viewModelScope.launch {
            try {
                // Получаем страну пользователя
                val country =
                    user.countryID?.let { countryId ->
                        countriesRepository.getCountryById(countryId.toString())
                    }
                // Получаем город пользователя
                val city =
                    user.cityID?.let { cityId ->
                        countriesRepository.getCityById(cityId.toString())
                    }
                _uiState.setValueIfChanged(ProfileUiState.Success(country, city)) {
                    logger.i(TAG, "Адрес для профиля обновлен из countriesRepository")
                }
            } catch (e: Exception) {
                val message = "Ошибка загрузки адреса для профиля: ${e.message}"
                _uiState.update { ProfileUiState.Error(message) }
                logger.e(TAG, message)
            }
        }
    }
}
```

### Ключевые моменты в ProfileViewModel:

1. **Отдельное состояние обновления:**
   ```kotlin
   private val _isRefreshing = MutableStateFlow(false)
   val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
   ```

2. **Метод refreshProfile() с логированием и finally:**
   ```kotlin
   fun refreshProfile() {
       val user = currentUser.value ?: run {
           logger.w(TAG, "Пропускаем обновление профиля: пользователь не авторизован")
           return
       }

       viewModelScope.launch {
           try {
               _isRefreshing.update { true }
               logger.i(TAG, "Начало обновления профиля: ${user.id}")
               loadSocialUpdates(user.id, updateUiState = false)
           } catch (e: Exception) {
               val errorMessage = "Ошибка обновления профиля: ${e.message}"
               logger.e(TAG, errorMessage)
           } finally {
               _isRefreshing.update { false }
           }
       }
   }
   ```

3. **Параметр updateUiState = false** для обновления без изменения основного UI State

## Полная реализация ProfileRootScreen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRootScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    appContainer: AppContainer? = null,
    appState: AppState? = null,
    onShowLoginSheet: () -> Unit = {}
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
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshProfile() },
            state = pullRefreshState,
            modifier = modifier.fillMaxSize(),
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
                                shortAddress = stringResource(R.string.loading)
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
                            appState?.navController?.navigate(Screen.MyFriends.route)
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
                            appState?.navController?.navigate(
                                Screen.UserParks.createRoute(user.id)
                            )
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

                // Spacer прижимает кнопку выхода к низу экрана
                Spacer(modifier = Modifier.weight(1f))

                // Кнопка "Выйти"
                LogoutButton(
                    onClick = {
                        showLogoutDialog = true
                    }
                )
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
```

### Ключевые моменты в ProfileRootScreen:

1. **Подписка на isRefreshing:**
   ```kotlin
   val isRefreshing by viewModel.isRefreshing.collectAsState()
   ```

2. **Создание состояния pull-to-refresh:**
   ```kotlin
   val pullRefreshState = rememberPullToRefreshState()
   ```

3. **PullToRefreshBox с кастомным индикатором:**
   ```kotlin
   PullToRefreshBox(
       isRefreshing = isRefreshing,
       onRefresh = { viewModel.refreshProfile() },
       state = pullRefreshState,
       modifier = modifier.fillMaxSize(),
       indicator = {
           PullToRefreshDefaults.Indicator(
               state = pullRefreshState,
               isRefreshing = isRefreshing,
               modifier = Modifier
                   .align(Alignment.TopCenter)
                   .padding(top = dimensionResource(R.dimen.spacing_regular))
           )
       }
   )
   ```

4. **Вертикальная прокрутка контента:**
   ```kotlin
   Column(
       modifier = Modifier
           .fillMaxSize()
           .verticalScroll(rememberScrollState())
           .padding(dimensionResource(R.dimen.spacing_regular)),
       verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
   )
   ```

## Пример для списков (LazyColumn)

При работе с LazyColumn используй `LazyColumn` вместо `Column`, но структура остается аналогичной:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksListScreen(
    modifier: Modifier = Modifier,
    viewModel: ParksViewModel,
) {
    val parks by viewModel.parks.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshParks() },
        state = pullRefreshState,
        modifier = modifier.fillMaxSize(),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.spacing_regular),
                end = dimensionResource(R.dimen.spacing_regular),
                top = dimensionResource(R.dimen.spacing_small),
                bottom = dimensionResource(R.dimen.spacing_regular)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            items(parks, key = { it.id }) { park ->
                ParkRowView(
                    park = park,
                    onClick = { /* Обработка клика */ }
                )
            }
        }
    }
}
```

### Ключевые отличия для LazyColumn:

- Используй `LazyColumn` вместо `Column`
- Применяй отступы через `contentPadding` (но не используй `calculateTopPadding()`)
- LazyColumn сам поддерживает прокрутку, `verticalScroll` не нужен

## Unit-тест для ViewModel

Пример unit-теста для проверки работы pull-to-refresh:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var countriesRepository: CountriesRepository
    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var errorReporter: ErrorReporter
    private lateinit var profileViewModel: ProfileViewModel

    @Before
    fun setup() {
        countriesRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)

        profileViewModel = ProfileViewModel(
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            logger = logger,
            errorReporter = errorReporter
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun refreshProfile_whenCalled_thenUpdatesIsRefreshing() = runTest {
        // Given - настроен mock для пользователя
        val testUser = User(id = 1L, name = "Test User")
        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(testUser)
        coEvery { swRepository.getSocialUpdates(any()) } returns Result.success(
            SocialUpdates(user = testUser)
        )

        // When
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then - проверяем, что состояние обновления сброшено
        assertFalse(profileViewModel.isRefreshing.value)
    }

    @Test
    fun refreshProfile_whenNotAuthorized_doesNothing() = runTest {
        // Given - пользователь null
        coEvery { swRepository.getCurrentUserFlow() } returns flowOf(null)

        // When
        profileViewModel.refreshProfile()
        advanceUntilIdle()

        // Then - проверяем, что getSocialUpdates не вызывался
        coVerify(exactly = 0) { swRepository.getSocialUpdates(any()) }
    }
}
```

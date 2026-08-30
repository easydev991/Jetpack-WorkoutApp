# ViewModel Testing

ViewModel тестируются **только** в `app/src/test/` (unit-тесты).
Integration-тесты ViewModel в `androidTest/` запрещены.

## advanceUntilIdle

**После каждого действия ViewModel** в `runTest`:

```kotlin
viewModel.doSomething()
advanceUntilIdle()        // <- обязательно
assertEquals(expected, viewModel.uiState.value)
```

`StandardTestDispatcher` буферизирует; `UnconfinedTestDispatcher` (по
умолчанию в нашем `MainDispatcherRule`) выполняет eagerly — но
`advanceUntilIdle()` всё равно нужен для вложенных корутин.

## MainDispatcherRule

Живёт в `app/src/test/java/com/swparks/ui/viewmodel/MainDispatcherRule.kt`:

```kotlin
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

Использование:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: AuthViewModel
    private val repository: AuthRepository = FakeAuthRepository()

    @Before
    fun setup() { viewModel = AuthViewModel(repository) }

    @Test
    fun login_whenValidCredentials_thenStateIsAuthenticated() = runTest {
        // Given
        repository.setState(AuthState.Idle)

        // When
        viewModel.login(LoginCredentials("user@test.com", "pwd"))

        // Then
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AuthState.Authenticated)
    }
}
```

Без `MainDispatcherRule` `viewModelScope.launch` использует реальный
`Dispatchers.Main` и не контролируется тестом → зависает или
assert читает стейт до того, как корутина отработала.

## StateFlow с WhileSubscribed

Если ViewModel отдаёт `StateFlow` с `SharingStarted.WhileSubscribed`,
upstream не стартует без подписчика — тест без подписки не видит
эмиссий. Решение: подписаться в `backgroundScope.launch` до
`advanceUntilIdle`:

```kotlin
@Test
fun observeState_whenSubscribed_thenEmitsInitialValue() = runTest {
    // Given
    viewModel.startObserving()

    // When
    backgroundScope.launch { viewModel.uiState.collect() }
    advanceUntilIdle()

    // Then
    assertEquals(initial, viewModel.uiState.value)
}
```

## Turbine для SharedFlow/StateFlow

Turbine доступен только в `app/src/test/` (зависимость
`testImplementation(libs.turbine)` в `app/build.gradle.kts`). Если
нужен в androidTest — добавить `androidTestImplementation` явно
(сейчас 0 файлов, не используется).

```kotlin
@Test
fun events_whenEmittedByViewModel_thenReceivedByTurbine() = runTest {
    // When
    viewModel.events.test {
        viewModel.doSomething()
        advanceUntilIdle()

        // Then
        val event = awaitItem()
        assertEquals(expectedEvent, event)
        cancelAndIgnoreRemainingEvents()
    }
}
```

Импорт: `import app.cash.turbine.test`.

## SavedStateHandle

Передаётся в конструктор через factory или напрямую:

```kotlin
private val savedState = SavedStateHandle(mapOf("key" to "value"))
viewModel = MyViewModel(repository, savedState)
```

Чтение `savedState.getStateFlow("key", default)` работает в `runTest`
после `MainDispatcherRule`.

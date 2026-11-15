# Use Cases

Use Case в проекте возвращает `Result<T>`. Юнит-тесты — в
`app/src/test/java/com/swparks/domain/usecase/`, ~20 файлов
(LoginUseCaseTest, ChangePasswordUseCaseTest, SyncParksUseCaseTest,
CreateJournalUseCaseTest, ...).

## Каркас

Полный `setup()`/`tearDown()` с `mockkStatic(Log::class)` —
`references/mocking-mockk.md` (раздел «Mocking Android Log»).
Анатомия `LoginUseCaseTest` (Given/When/Then, сообщения assert) —
`references/fundamentals.md`.

Из `LoginUseCaseTest.kt`:

```kotlin
class LoginUseCaseTest {
    private lateinit var loginUseCase: LoginUseCase
    private val tokenEncoder: TokenEncoder = mockk(relaxed = true)
    private val secureTokenRepository: SecureTokenRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val preferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val crashReporter: CrashReporter = NoOpCrashReporter()
    private val testCredentials =
        LoginCredentials(login = "user@test.com", password = "password123")

    @Before
    fun setup() {
        loginUseCase = LoginUseCase(
            tokenEncoder,
            secureTokenRepository,
            authRepository,
            preferencesRepository,
            crashReporter
        )
    }
}
```

## Assert-паттерны

Из `LoginUseCaseTest.kt` (специфика use-case — `Result<T>`,
`coVerify(exactly = 0)`):

```kotlin
@Test
fun invoke_whenInvalidCredentials_thenFailureAndNoSave() = runTest {
    // Given
    coEvery { authRepository.login(any()) } returns Result.failure(AuthError.InvalidCredentials)

    // When
    val result = loginUseCase(testCredentials)

    // Then
    assertTrue(result.isFailure)
    coVerify(exactly = 0) { secureTokenRepository.save(any()) }
}
```

## Извлечение из Result

```kotlin
val userId = result.getOrNull()?.userId
val error = result.exceptionOrNull()
```

## Use Case с Flow

`runTest` поддерживает Flow в Use Case — `first()` / `toList()`.
Фильтр-стейт (как `parksFilter`) подменяется **Fake на
`MutableStateFlow`** (см. `references/fakes-and-stateflow.md`,
раздел «Fake*Repository в unit»), а не `every { parksFilter.value }` —
это даёт реальные эмиссии, доезжающие до подписчика.

```kotlin
class FakeParksFilterRepository : ParksFilterRepository {
    private val _filter = MutableStateFlow(ParkFilter())
    override val filter: StateFlow<ParkFilter> = _filter
    fun setFilter(new: ParkFilter) { _filter.value = new }
}

@Test
fun observeParks_whenFilterCityIsSet_thenEmitsFiltered() = runTest {
    // Given
    val filterRepo = FakeParksFilterRepository()
    val observeParks = ObserveParksUseCase(filterRepo, parksRepository)

    backgroundScope.launch { observeParks().collect() }   // подписка до advanceUntilIdle

    // When
    filterRepo.setFilter(ParkFilter(cityId = 1))
    advanceUntilIdle()

    // Then
    val result = observeParks().first()
    assertEquals(1, result.size)
}
```

## Что НЕ делать

- Не подменять настоящий `CrashReporter` mockk'ом — есть `NoOpCrashReporter`
  (используй его).
- В **unit-тестах** не делать `runBlocking` для suspend — `runTest { ... }`.
  В **androidTest** `runBlocking { db.insert(...) }` допустим для
  сидирования данных, если за ним идёт `composeTestRule.waitForIdle()`
  (см. `references/compose-ui-testing.md`, раздел «Ожидание данных»).

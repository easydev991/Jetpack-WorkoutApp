# Fakes and StateFlow

Fake-репозитории на `MutableStateFlow` и `MutableSharedFlow` — для
androidTest-изоляции UI от реального DI и для unit-тестов Flow-эмиссий
(когда MockK + `flowOf(...)` не доезжает до подписчика).

## Fake*ViewModel в androidTest

В `app/src/androidTest/.../ui/viewmodel/` живут 9 `Fake*` (список ниже —
канон; `MutableSharedFlow` есть у 8 из них, нет только в `FakeProfileViewModel`):

- `FakeParksRootViewModel.kt`
- `FakeLoginViewModel.kt`
- `FakeEventsViewModel.kt`
- `FakeEventFormViewModel.kt`
- `FakeJournalEntriesViewModel.kt`
- `FakeJournalsViewModel.kt`
- `FakeParkFormViewModel.kt`
- `FakeProfileViewModel.kt` — только `StateFlow`, без `SharedFlow` (задаётся конструктором)
- `FakeTextEntryViewModel.kt` — только `SharedFlow`, без `MutableStateFlow`

Исключение, не в общей коллекции: `FakeParkDetailViewModel` — `private class`
внутри `ParkDetailScreenTest.kt` (не файл в `ui/viewmodel/`).

### Каркас

Из `FakeParksRootViewModel.kt`:

```kotlin
class FakeParksRootViewModel : IParksRootViewModel {
    private val _uiState = MutableStateFlow(ParksRootUiState(isLoadingFilter = false))
    override val uiState: StateFlow<ParksRootUiState> = _uiState

    // Буфер — см. «Буфер MutableSharedFlow» ниже.
    private val _events = MutableSharedFlow<ParksRootEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<ParksRootEvent> = _events.asSharedFlow()

    private val _parksFilter = MutableStateFlow(ParkFilter())
    override val parksFilter: StateFlow<ParkFilter> = _parksFilter

    // хелпер для теста: собрать все эмитированные события
    private val _capturedEvents = mutableListOf<ParksRootEvent>()
    val capturedEvents: List<ParksRootEvent> = _capturedEvents

    fun reset() {
        _uiState.value = ParksRootUiState(isLoadingFilter = false)
        _capturedEvents.clear()
    }
}
```

**Важно**: при изменении интерфейса реального ViewModel — синхронизируй
Fake. Иначе UI-тест компилируется, но ловит неактуальное поведение.

## Буфер MutableSharedFlow

`MutableSharedFlow()` без буфера + `suspend emit()` до подписки
(`LaunchedEffect` на UI) → deadlock runBlocking ↔ collect. Реальный
случай: `FakeParkDetailViewModel` в `ParkDetailScreenTest.kt` (см.
`docs/ui-tests-optimization.md`, «Находка» п.1) — тест висел >14 мин.

Защита (выбрать одно):
- `MutableSharedFlow(extraBufferCapacity = 1)` (использует
  `FakeParksRootViewModel`, `FakeEventsViewModel`) — эмиссия буферизуется,
  доезжает до поздно подписавшегося.
- `MutableSharedFlow(replay = 1)` (`FakeJournalsViewModel`) — последнее
  значение сохраняется для нового подписчика.

**Существующие Fake***: 5 из 9 `Fake*` с голым `MutableSharedFlow<T>()`
(`FakeEventFormViewModel`, `FakeJournalEntriesViewModel`, `FakeLoginViewModel`,
`FakeParkFormViewModel`, `FakeTextEntryViewModel`) — у них нет `suspend emit()`
в коде (только накопление или test-helpers), deadlock-риск отсутствует,
миграция не запланирована.

## Fake*Repository в unit

Для unit-тестов с Flow — минимальный Fake на `MutableStateFlow`:

```kotlin
class FakeAuthRepository : AuthRepository {
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    override val state: StateFlow<AuthState> = _state

    override suspend fun login(credentials: LoginCredentials): Result<LoginSuccess> {
        _state.value = AuthState.Loading
        // эмулируем сетевой вызов
        delay(10)
        val success = LoginSuccess(userId = 42L, token = "fake-token")
        _state.value = AuthState.Authenticated(success)
        return Result.success(success)
    }

    fun setState(new: AuthState) { _state.value = new }
}
```

Подписчик в ViewModel получает все эмиссии. Главное — **создать
подписку до `advanceUntilIdle()`** (см. `references/viewmodel-testing.md`).

## Bottom line

- Репозиторий с Flow → Fake (StateFlow/SharedFlow).
- API/Logger/Provider (stateless) → MockK.
- `Fake*ViewModel` для нового экрана — добавить в
  `app/src/androidTest/.../ui/viewmodel/` (см. список выше).
- Для `MutableSharedFlow` — см. раздел «Буфер MutableSharedFlow» выше.

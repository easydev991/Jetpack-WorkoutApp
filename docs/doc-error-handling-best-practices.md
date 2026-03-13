# Best Practices: Обработка ошибок в Kotlin Coroutines

Документ содержит правила обработки исключений, основанные на регрессиях после коммита b1f022e. Нарушение этих правил приводит к необработанным исключениям и сломанному UX.

---

## Правило 1: HttpException требует отдельного catch

### Проблема

После сужения `catch (Exception)` до `catch (IOException)` в Retrofit-вызовах, `HttpException` (ошибки HTTP 4xx/5xx) начали "пролетать" необработанными.

### Почему это происходит

- `HttpException` — не наследник `IOException`
- `IOException` ловит только сетевые ошибки (нет сети, timeout)
- HTTP-ошибки (401, 404, 500) — это отдельный класс `retrofit2.HttpException`

### Правильный паттерн в репозиториях

```kotlin
suspend fun refreshData(): Result<Unit> = try {
    val response = api.getData()
    dao.insert(response)
    Result.success(Unit)
} catch (e: IOException) {
    Log.e(TAG, "Сетевая ошибка: ${e.message}")
    Result.failure(NetworkException("Нет соединения", e))
} catch (e: HttpException) {
    Log.e(TAG, "HTTP ошибка ${e.code()}: ${e.message}")
    Result.failure(e)
}
```

### Правильный паттерн в ViewModel

```kotlin
viewModelScope.launch {
    try {
        repository.loadData()
        _uiState.value = UiState.Success
    } catch (e: IOException) {
        _uiState.value = UiState.Error(e.message ?: "Сетевая ошибка")
    } catch (e: HttpException) {
        _uiState.value = UiState.Error("Ошибка сервера: ${e.code()}")
    }
}
```

---

## Правило 2: CancellationException нельзя проглатывать

### Проблема

При использовании `catch (e: Exception)` в `viewModelScope.launch` корутины перестают корректно отменяться.

### Почему это критично

- `CancellationException` используется Kotlin для механизма отмены корутин
- Если поймать и не пробросить `CancellationException`, корутина "зомби" — не отменяется
- Это приводит к memory leaks и неожиданному поведению

### Правильный паттерн

```kotlin
viewModelScope.launch {
    try {
        val data = repository.getData()
        _uiState.value = UiState.Success(data)
    } catch (e: Exception) {
        // КРИТИЧНО: всегда пробрасываем CancellationException
        if (e is CancellationException) throw e
        
        // Обрабатываем остальные ошибки
        Log.e(TAG, "Ошибка: ${e.message}")
        _uiState.value = UiState.Error(e.message ?: "Неизвестная ошибка")
    }
}
```

### С подавлением detekt warning

```kotlin
@Suppress("InstanceOfCheckForException")
class SomeViewModel : ViewModel() {
    
    fun loadData() {
        viewModelScope.launch {
            try {
                // ...
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // обработка
            }
        }
    }
}
```

---

## Правило 3: TimeoutCancellationException — особый случай

### Проблема

`TimeoutCancellationException` уже является подклассом `CancellationException`, поэтому отдельный `catch (e: CancellationException)` избыточен и вызывает detekt warning `RethrowCaughtException`.

### Неправильно

```kotlin
try {
    withTimeout(5000) { ... }
} catch (e: CancellationException) {
    throw e  // Detekt: RethrowCaughtException
} catch (e: TimeoutCancellationException) {
    // Этот блок недостижим!
}
```

### Правильно

```kotlin
try {
    withTimeout(5000) { ... }
} catch (e: TimeoutCancellationException) {
    // TimeoutCancellationException уже CancellationException
    Log.e(TAG, "Таймаут операции")
    Result.failure(e)
}
```

---

## Правило 4: Result vs UI State

### Репозитории — возвращают Result

```kotlin
// Repository
suspend fun loadData(): Result<Data> = try {
    val response = api.getData()
    Result.success(response)
} catch (e: IOException) {
    Result.failure(NetworkException("Нет сети", e))
} catch (e: HttpException) {
    Result.failure(e)
}
```

### ViewModel — использует sealed UI State

```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ViewModel
fun load() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        
        when (val result = repository.loadData()) {
            is Result.Success -> _uiState.value = UiState.Success(result.data)
            is Result.Failure -> _uiState.value = UiState.Error(result.exception.message ?: "Ошибка")
        }
    }
}
```

---

## Чек-лист для code review

При ревью кода с exception handling проверяй:

### В репозиториях

- [ ] `catch (IOException)` присутствует для сетевых ошибок
- [ ] `catch (HttpException)` присутствует для HTTP ошибок
- [ ] Возвращается `Result.failure(...)` а не throw
- [ ] Логируется ошибка перед возвратом

### В ViewModel с viewModelScope.launch

- [ ] Если есть `catch (e: Exception)` — проверь наличие `if (e is CancellationException) throw e`
- [ ] UI state корректно обновляется при ошибке
- [ ] One-off events (toasts, navigation) идут через Channel, не через State

### При использовании withTimeout

- [ ] Не дублируй catch для `CancellationException` и `TimeoutCancellationException`
- [ ] `TimeoutCancellationException` лови явно если нужна специфическая обработка

---

## Типичные ошибки

| Ошибка | Симптом | Исправление |
|--------|---------|-------------|
| Только `catch (IOException)` | При HTTP 500 приложение крашится | Добавить `catch (HttpException)` |
| `catch (Exception)` без проверки CancellationException | Корутины не отменяются, memory leaks | Добавить `if (e is CancellationException) throw e` |
| Двойной catch для CancellationException | Detekt warning `RethrowCaughtException` | Убрать избыточный catch |
| throw в репозитории вместо Result.failure | Unhandled exception в ViewModel | Возвращать Result |

---

## Тестовое покрытие

Для каждого обработчика ошибок нужны тесты:

```kotlin
@Test
fun loadData_onHttpException_returnsFailure() = runTest {
    // Given
    coEvery { api.getData() } throws HttpException(Response.error<Any>(500, "".toResponseBody()))

    // When
    val result = repository.loadData()

    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is HttpException)
}

@Test
fun loadData_onCancellationException_propagates() = runTest {
    // Given
    val viewModel = SomeViewModel(repository)
    
    // When
    viewModel.loadData()
    advanceUntilIdle()
    
    // Then - корутина должна корректно отмениться при cancel
    // (проверяется через тестирование поведения при отмене)
}
```

---

## История изменений

| Дата | Коммит | Изменение |
|------|--------|-----------|
| 2025-03 | b1f022e → fix | Добавлен catch (HttpException) в репозитории и ViewModel |
| 2025-03 | 56fbfc6 | Добавлен `if (e is CancellationException) throw e` в 7 ViewModel |

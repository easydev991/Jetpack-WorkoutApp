# Анализ и план реализации универсального механизма обработки ошибок

## Обзор

### Проблема

В текущей архитектуре каждая ViewModel должна отдельно реализовывать логику для:
1. Логирования ошибок
2. Отправки ошибок в UI через Channel/SharedFlow
3. Обработки ошибок в UI компонентах для отображения пользователю

Это приводит к:
- Дублированию кода в разных ViewModels
- Рискам пропустить обработку ошибок на каком-то экране
- Разным UX для ошибок на разных экранах

### Цель

Создать единый механизм для отображения ошибок пользователям, который:
- Централизованно обрабатывает ошибки из всех ViewModels
- Показывает уведомления (Snackbar/Toast/Alert) поверх текущего экрана
- Обеспечивает консистентный UX для всех ошибок приложения
- Легко интегрируется в существующую архитектуру через DI
- Не нарушает существующие паттерны создания ViewModels

---

## Рекомендуемый подход: ErrorReporter + SharedFlow

### Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                     ViewModels                          │
│  (FriendsListViewModel, LoginViewModel, etc.)          │
│                                                         │
│  swRepository.method()                                  │
│      .onFailure { error ->                              │
│          errorReporter.handleError(AppError.Network(...)) │
│      }                                                  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓ handleError()
┌─────────────────────────────────────────────────────────┐
│                 ErrorHandler : ErrorReporter              │
│                                                         │
│  - logger: Logger                                       │
│  + errorFlow: SharedFlow<AppError>                      │
│  + handleError(error: AppError)  // tryEmit без launch  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓ emit(error)
┌─────────────────────────────────────────────────────────┐
│                      RootScreen                         │
│                                                         │
│  LaunchedEffect(Unit) {                                │
│      errorReporter.errorFlow.collect { error ->         │
│          val message = error.toMessage()               │
│          snackbarHostState.showSnackbar(message)       │
│      }                                                  │
│  }                                                      │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ↓ showSnackbar()
┌─────────────────────────────────────────────────────────┐
│                 SnackbarHostState                       │
│                                                         │
│  Показывает Snackbar поверх текущего экрана            │
└─────────────────────────────────────────────────────────┘
```

### Преимущества подхода

1. **Чистое разделение ответственности**
   - AppState остается только для навигации и авторизации
   - ErrorReporter - отдельный интерфейс для обработки ошибок
   - ErrorHandler - реализация интерфейса с логикой
   - Нет смешения UI-state и business-logic concerns

2. **Легкая интеграция в текущую архитектуру**
   - ErrorReporter размещается рядом с Logger и другими сервисами
   - Передается в ViewModels через существующие factory методы в AppContainer
   - Не требует изменений в AppState

3. **Простота использования в ViewModels**
   - Использует `tryEmit` без необходимости в `launch` и scope
   - `MutableSharedFlow` с `extraBufferCapacity` обеспечивает надежную отправку
   - ViewModel может вызывать `handleError` напрямую из корутины

4. **Расширяемость**
   - Логирование ошибок в одном месте
   - Легко добавить аналитику (Firebase, Crashlytics)
   - Можно добавить историю ошибок для отладки

5. **Тестируемость**
   - Легко мокать ErrorReporter в тестах ViewModels (интерфейс)
   - Отдельные unit-тесты для ErrorHandler
   - Нет зависимости от AppState и Compose

**Реализовано:**
- Интерфейс `ErrorReporter` с `errorFlow` и `handleError()`
- Класс `ErrorHandler` с `MutableSharedFlow` (buffer=10, DROP_OLDEST)
- Логирование всех типов ошибок через Logger

---

## План реализации

### Этапы 1-7: Создание модели, ErrorReporter, интеграция и миграция ✅ ВЫПОЛНЕНО

**Создание и интеграция механизма обработки ошибок:**
- ✅ Создан `AppError` (Network, Validation, Server, Generic) с 13 unit-тестами
- ✅ Реализованы интерфейс `ErrorReporter`, класс `ErrorHandler` (SharedFlow, buffer=10, DROP_OLDEST) с 7 unit-тестами
- ✅ Интегрирован ErrorReporter в AppContainer, обновлены factory методы для всех ViewModels, созданы 3 unit-теста
- ✅ Обновлены ViewModels:
  - `FriendsListViewModel`: добавлен errorReporter, заменены вызовы handleError в onAcceptFriendRequest/onDeclineFriendRequest, 8 unit-тестов
  - `LoginViewModel`: добавлен errorReporter, заменены ошибки валидации и сетевые ошибки, обновлены login()/resetPassword()
  - `EventsViewModel`: добавлен errorReporter, заменены исключения на handleError(), 6 unit-тестов
  - `AuthViewModel`: добавлен errorReporter, добавлен вызов handleError(), 5 unit-тестов
- ✅ Реализован Snackbar в RootScreen: добавлен SnackbarHostState, сбор ошибок из errorFlow через LaunchedEffect, логирование, 4 инструментальных теста
- ✅ Все unit-тесты и инструментальные тесты (67) проходят, проект собирается без ошибок

---

### Этап 8: Локализация сообщений об ошибках ✅ ВЫПОЛНЕНО

**Реализовано:**

- ✅ Создан `AppErrorExt.kt` с extension-функцией `toUiText()` для маппинга ошибок на локализованные строки
  - Сетевые ошибки: IOException → error_network_io, остальные → error_network_general
  - Ошибки валидации: email → error_validation_email, password → error_validation_password, остальные → параметризованная строка
  - Ошибки сервера: 401, 403, 404, 500, 503 и другие → соответствующие локализованные сообщения
  - Generic ошибки: сохраняется оригинальное сообщение
  - Разделение на приватные функции для снижения cyclomatic complexity
  - Константы для HTTP кодов (избегание magic numbers)

- ✅ Добавлены строковые ресурсы в `values/strings.xml` и `values-ru/strings.xml`:
  - error_network_io, error_network_general
  - error_validation_field, error_validation_email, error_validation_password
  - error_server_unauthorized, error_server_forbidden, error_server_not_found, error_server_internal, error_server_unavailable, error_server_general
  - error_unknown

- ✅ Обновлен `RootScreen.kt`:
  - Использование `error.toUiText(context)` вместо прямого `error.message`
  - Импорт extension-функции `toUiText`, удаление неиспользуемого импорта `AppError`

**Результаты:**
- ✅ Проект собирается без ошибок, все unit-тесты проходят
- ✅ Код отформатирован (ktlint, detekt)
- ✅ Сообщения об ошибках локализованы для английского и русского языков
- ✅ Параметризованные сообщения поддерживаются

---

### Этап 9: Тестирование на устройстве

**Задачи:**
1. Собрать проект (`./gradlew assembleDebug`)
2. Запустить приложение на устройстве/эмуляторе
3. Протестировать сценарии с ошибками сети:
   - Принятие/отклонение заявки без интернета
   - Загрузка данных без интернета
   - Ошибки сервера
   - Ошибки валидации
4. Проверить, что ошибки корректно отображаются через Snackbar
5. Проверить, что UI не крашится при ошибках
6. Проверить, что несколько ошибок корректно отображаются по очереди
7. Проверить, что буфер ошибок работает корректно (DROP_OLDEST)
8. Проверить локализацию сообщений (переключение языка)

**Чек-лист тестирования:**
- [ ] Отключить интернет, попытаться принять заявку → Snackbar с ошибкой сети
- [ ] Отключить интернет, попытаться войти → Snackbar с ошибкой сети
- [ ] Войти с неверными данными → Ошибка валидации под полем + Snackbar
- [ ] Имитировать ошибку сервера (Mock API) → Snackbar с ошибкой сервера
- [ ] Отправить несколько ошибок подряд → Проверить, что все отображаются
- [ ] Проверить, что приложение не крашится при ошибках
- [ ] Проверить локализацию сообщений (английский/русский)

---

## Дополнительные улучшения (будущие итерации)

### Улучшение 1: AlertDialog для критических ошибок

**Идея:** Разные типы UI для разных типов ошибок

- **Snackbar** для информационных ошибок (Network, Validation)
- **AlertDialog** для критических ошибок (Server 500, Auth)

**Реализация:**

```kotlin
// RootScreen.kt
val showErrorDialog by remember { mutableStateOf<AppError?>(null) }

LaunchedEffect(Unit) {
    appContainer.errorReporter.errorFlow.collect { error ->
        when (error) {
            is AppError.Server, is AppError.Generic -> {
                showErrorDialog = error
            }
            else -> {
                // Snackbar для остальных ошибок
                val message = error.toUiText(context)
                snackbarHostState.showSnackbar(message)
            }
        }
    }
}

// AlertDialog для критических ошибок
showErrorDialog?.let { error ->
    AlertDialog(
        onDismissRequest = { showErrorDialog = null },
        title = { Text("Ошибка") },
        text = { Text(error.toUiText(context)) },
        confirmButton = {
            Button(onClick = { showErrorDialog = null }) {
                Text("OK")
            }
        }
    )
}
```

### Улучшение 2: Кнопка "Повторить" в Snackbar

**Идея:** Добавить действие для повторения операции

> **ПРИМЕЧАНИЕ:** Это улучшение для будущей итерации. В текущей реализации (Этап 1) поле `retryAction` отсутствует в `AppError.Network`.

```kotlin
// AppError.kt (будущая версия)
sealed class AppError {
    data class Network(
        val message: String,
        val throwable: Throwable? = null,
        val retryAction: (() -> Unit)? = null  // ✅ Добавлено в будущей итерации
    ) : AppError()
    // ...
}

// RootScreen.kt
LaunchedEffect(Unit) {
    appContainer.errorReporter.errorFlow.collect { error ->
        val message = error.toUiText(context)
        val actionLabel = if (error is AppError.Network && error.retryAction != null) {
            "Повторить"
        } else {
            null
        }

        val snackbarResult = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            (error as? AppError.Network)?.retryAction?.invoke()
        }
    }
}
```

### Улучшение 3: Кэш истории ошибок

**Идея:** Хранить историю ошибок для аналитики и отладки

```kotlin
// ErrorHandler.kt
class ErrorHandler(
    private val logger: Logger,
) : ErrorReporter {
    private val errorHistory = mutableListOf<AppError>()

    override fun handleError(error: AppError): Boolean {
        // Сохраняем в историю
        errorHistory.add(error)

        // Ограничиваем размер истории
        if (errorHistory.size > MAX_HISTORY_SIZE) {
            errorHistory.removeAt(0)
        }

        // Логируем и отправляем
        logger.e(TAG, error.message)
        return _errorFlow.tryEmit(error)
    }

    fun getErrorHistory(): List<AppError> = errorHistory.toList()

    fun clearErrorHistory() {
        errorHistory.clear()
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 100
    }
}
```

### Улучшение 4: Аналитика ошибок

**Идея:** Отправлять ошибки в аналитику (Firebase Analytics, Crashlytics)

```kotlin
// AnalyticsHelper.kt
class AnalyticsHelper {
    fun logError(error: AppError) {
        when (error) {
            is AppError.Network -> {
                Firebase.analytics.logEvent("network_error") {
                    param("message", error.message)
                }
            }
            is AppError.Server -> {
                Firebase.crashlytics.recordException(
                    RuntimeException(error.message)
                )
            }
            // ...
        }
    }
}

// ErrorHandler.kt
class ErrorHandler(
    private val logger: Logger,
    private val analyticsHelper: AnalyticsHelper,  // ✅ Добавлено
) : ErrorReporter {
    override fun handleError(error: AppError): Boolean {
        logger.e(TAG, error.message)
        analyticsHelper.logError(error)  // ✅ Логируем в аналитику
        return _errorFlow.tryEmit(error)
    }
}
```

---

## Критерии приемки

- [x] **Этап 1**: AppError создан и покрыт unit-тестами
- [x] **Этап 2**: ErrorReporter интерфейс и ErrorHandler реализация созданы
- [x] **Этап 3**: AppContainer обновлен с errorReporter и всеми factory методами
- [x] **Этап 4**: FriendsListViewModel обновлен с errorReporter
- [x] **Этап 5**: LoginViewModel обновлен с errorReporter
- [x] **Этап 6**: RootScreen показывает Snackbar при ошибках + инструментальные тесты
- [x] **Этап 7**: Все существующие ViewModels обновлены (EventsViewModel, AuthViewModel)
- [x] **Этап 8**: Сообщения об ошибках локализованы с AppError.toUiText()
- [ ] **Этап 9**: Функциональность протестирована на устройстве
- [x] Проект собирается без ошибок (`./gradlew assembleDebug`)
- [x] Unit-тесты для Этапов 1-7 проходят (`./gradlew testDebugUnitTest`)
- [x] Все инструментальные тесты проходят (`./gradlew connectedAndroidTest`)
- [x] `make format` выполнен (ktlint, detekt)

---

## Заключение

Реализация универсального механизма обработки ошибок через **ErrorReporter (интерфейс) + ErrorHandler (реализация) + SharedFlow** позволит:

1. **Устранить дублирование кода** в ViewModels
2. **Обеспечить консистентный UX** для всех ошибок приложения
3. **Упростить разработку новых экранов** - нет необходимости реализовывать обработку ошибок с нуля
4. **Улучшить тестируемость** - единый механизм для всех ошибок, интерфейс для моков
5. **Сохранить архитектурную чистоту** - минимальные изменения в существующем коде
6. **Сохранить чистоту AppState** - AppState остается только для навигации и авторизации
7. **Легкая интеграция** через существующий DI контейнер AppContainer
8. **Чистое разделение ответственности** - интерфейс для DI, реализация для логики

Этот подход идеально подходит для текущей архитектуры приложения и соответствует принципам MVVM и Clean Architecture. Разделение ответственности между AppState (UI-состояние) и ErrorReporter (обработка ошибок) обеспечивает чистую архитектуру и легкую поддержку кода. Использование интерфейса ErrorReporter в зависимостях улучшает тестируемость и соответствует принципу Dependency Inversion.

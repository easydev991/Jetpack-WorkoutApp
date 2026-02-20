# LoginScreen (экран авторизации)

## Обзор

Экран авторизации позволяет войти в workout.su или восстановить пароль.

**Важно:** Экран авторизации выполняет ТОЛЬКО функцию авторизации (вход в систему). Загрузка данных пользователя выполняется в ProfileViewModel при открытии профиля. Это разделение ответственности соответствует архитектуре приложения.

Источник истины для UI (iOS): `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/LoginScreen.swift`.

## Готово

### Архитектурные решения

**Одноразовые события через Channel:**

Для событий, которые должны обрабатываться только один раз (успешная авторизация, успешное восстановление пароля), реализован паттерн с использованием `Channel<LoginEvent>`:

- **LoginViewModel** содержит `_loginEvents: Channel<LoginEvent>` и публикует `loginEvents: Flow<LoginEvent>`
- При успешной авторизации отправляется событие `LoginEvent.Success(userId)`
- При успешном восстановлении пароля отправляется событие `LoginEvent.ResetSuccess(email)`
- **LoginScreen** подписывается на `viewModel.loginEvents` через `LaunchedEffect` и обрабатывает события
- После успешной операции UI state возвращается в `Idle`, предотвращая повторную обработку

Это предотвращает проблемы с повторной эмиссией событий при реконфигурации UI и позволяет корректно обрабатывать навигацию и уведомления.

### Авторизация (проверено)

- Токен сохраняется через `SecureTokenRepository` в `LoginUseCase` (есть unit-тесты).
- ID пользователя сохраняется через `UserPreferencesRepository` при успешном логине.
- Флаг авторизации сохраняется при успешном логине и сбрасывается при выходе (LogoutUseCase / SWRepository / UserPreferencesRepository).
- Обработка ошибок сети через `NetworkException` с отображением диалогового окна пользователю.

### Реализованные компоненты

**Data layer:**
- `SWRepository.login(token)` - авторизация на сервере
- `SWRepository.resetPassword(login)` - восстановление пароля
- `SecureTokenRepository` - безопасное хранение токена авторизации
- `UserPreferencesRepository` - хранение ID пользователя и флага авторизации
- `AuthInterceptor` (401) - обработка ошибок авторизации
- `TokenInterceptor` - добавление токена в заголовки запросов

**Domain layer:**
- `LoginUseCase` - бизнес-логика авторизации с сохранением токена и userId (реализует ILoginUseCase)
- `ResetPasswordUseCase` - бизнес-логика восстановления пароля (реализует IResetPasswordUseCase)
- `LoginUiState` - sealed class для управления состоянием UI
- `LoginEvent` - sealed class для одноразовых событий (Success, ResetSuccess)
- `LoginCredentials` - модель для валидации учетных данных
- `LoginSuccess` - модель результата авторизации

**UI layer:**
- `LoginScreen` - экран авторизации с полями логина и пароля
- `LoginSheetHost` - хост для LoginSheetHost в виде полноэкранного модального листа
- `LoginViewModel` - ViewModel с методом `resetForNewSession()` для очистки состояния при повторном открытии экрана
- `AuthViewModel` - альтернативная ViewModel для авторизации (используется в тестах)
- Design System компоненты: `SWTextField`, `SWButton`, `LoadingOverlayView`
- AlertDialog'ы для уведомлений: "Нет интернета", "Забыли пароль", "Успешное восстановление"

**UI layer:**
- `LoginScreen` - экран авторизации с полями логина и пароля
- `LoginViewModel` - ViewModel с методом `resetForNewSession()` для очистки состояния при повторном открытии экрана
- Design System компоненты: `SWTextField`, `SWButton`, `LoadingOverlayView`
- AlertDialog'ы для уведомлений: "Нет интернета", "Забыли пароль", "Успешное восстановление"

**Navigation/интеграция:**
- `LoginScreen` открывается как полноэкранный ModalBottomSheet через `LoginSheetHost`
- Закрытие листа разрешено только:
  - по нажатию на крестик в левом верхнем углу (только если `!uiState.isBusy`: не идет логин и не загружаются данные)
  - автоматически после успешной авторизации или восстановления пароля
- Закрытие по тапу вне области, свайпу вниз, системной кнопке/жесту "назад" — запрещено
- Все жесты блокируются на уровне контента через `Modifier.disableAllGestures()`
- Callback `onLoginSuccess(userId: Long)` для навигации на профиль после авторизации
- Callback `onResetSuccess(email: String)` для уведомления об успешном сбросе пароля (опционально)

### Этапы и тесты

- Этапы 1–4 завершены (Domain, локализация, UI, навигация/интеграция).
- Полноэкранное модальное отображение LoginSheetHost: закрытие только крестиком/после успеха, dismiss-жесты и back press запрещены.
- Реализована обработка одноразовых событий через `Channel<LoginEvent>` для корректной навигации и уведомлений.

**Unit-тесты (71 тест):**
- `SWRepositoryAuthTest.kt` - 17 тестов (тесты методов авторизации в SWRepository)
- `LoginCredentialsTest.kt` - 14 тестов (валидация учетных данных)
- `LoginViewModelTest.kt` - 13 тестов (тесты ViewModel авторизации)
- `LoginUiStateTest.kt` - 8 тестов (тесты UI state)
- `AuthViewModelTest.kt` - 4 теста (тесты AuthViewModel)
- `LoginUseCaseTest.kt` - 3 теста (тесты use case авторизации)
- `ResetPasswordUseCaseTest.kt` - 3 теста (тесты use case восстановления пароля)
- `LoginSuccessTest.kt` - 3 теста (тесты модели LoginSuccess)
- `AuthInterceptorTest.kt` - 6 тестов (тесты интерсептора авторизации)

**UI-тесты (15 тестов):**
- `LoginScreenTest.kt` - проверка отображения всех элементов экрана, валидация полей, работа кнопок и алертов

## Осталось (по порядку)

1. ~~Блокировка системного "назад" при загрузке (опционально).~~ ✅ Реализовано через `enabled=false` на кнопке закрытия во время загрузки
2. Проверить работу на устройстве/эмуляторе (в т.ч. поведение модального листа и закрытие по swipe).

**Примечание:** Получение названий стран/городов относится к экрану регистрации, а не к экрану авторизации.

## Замечания (кратко)

- Null-safety: не использовать `!!`, применять `?`, `?:`, `checkNotNull`, корректно логировать ошибки.
- Логи на русском, уровни Error/Info/Debug.
- Сеть: проверять интернет перед запросом, показывать понятные ошибки, в use case'ах использовать `Result<T>`.
- После правок запускать `make format` и придерживаться стиля проекта; UI/логика должны соответствовать iOS-референсу.

## Обработка ошибок сети ✅

**Диалоговое окно "Нет интернета":**
- Ошибки сети (`IOException`/`NetworkException`) отображаются через AlertDialog
- Проверка типа ошибки выполняется в `HandleLoginErrorsOnly` для `LoginUiState.LoginError` и `LoginUiState.ResetError`
- Текст ошибки берется из ресурсов:
  - Заголовок: `R.string.alert_no_connection_title`
  - Сообщение: `R.string.alert_no_connection_message`
- Алерт показывается через `NoInternetAlert` (аналогично `ForgotPasswordAlert` и `ResetSuccessAlert`)
- Пользователь закрывает алерт кнопкой OK

**Обработка в Use Cases:**
- `LoginUseCase` и `ResetPasswordUseCase` возвращают `Result<T>` с ошибкой
- Серверные ошибки (400, 401, 500) возвращаются как `ServerException` с сообщением от сервера
- Сетевые ошибки возвращаются как `NetworkException`

**Отображение ошибок авторизации и восстановления пароля:**
- Ошибки авторизации отображаются под полем пароля (через `loginErrorState`)
- Ошибки восстановления пароля отображаются под полем логина (через `resetErrorState`)
- Ошибки очищаются при следующем вводе данных или при успешной операции

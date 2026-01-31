# LoginScreen (экран авторизации)

## Обзор

Экран авторизации позволяет войти в workout.su или восстановить пароль.

Источник истины для UI (iOS): `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/LoginScreen.swift`.

## Готово

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
- `LoginUseCase` - бизнес-логика авторизации с сохранением токена и userId
- `ResetPasswordUseCase` - бизнес-логика восстановления пароля
- `LoginViewModel` - ViewModel для управления состоянием экрана авторизации
- `LoginUiState` - sealed class для управления состоянием UI
- `LoginCredentials` - модель для валидации учетных данных
- `LoginSuccess` - модель результата авторизации

**UI layer:**
- `LoginScreen` - экран авторизации с полями логина и пароля
- `LoginViewModel` - ViewModel с методом `resetForNewSession()` для очистки состояния при повторном открытии экрана
- Design System компоненты: `SWTextField`, `SWButton`, `LoadingOverlayView`
- AlertDialog'ы для уведомлений: "Нет интернета", "Забыли пароль", "Успешное восстановление"

**Navigation/интеграция:**
- Маршрут `LoginScreen` интегрирован в навигацию приложения
- Модальное отображение через ModalBottomSheet с запретом dismiss-жестов и back press
- Кнопка закрытия заблокирована во время загрузки
- Callback `onLoginSuccess(userId: Long)` для загрузки профиля после авторизации

### Этапы и тесты

- Этапы 1–4 завершены (Domain, локализация, UI, навигация/интеграция).
- Полноэкранное модальное отображение LoginScreen (ModalBottomSheet): закрытие только крестиком/после успеха, dismiss-жесты и back press запрещены.

**Unit-тесты (68 тестов):**
- `LoginViewModelTest.kt` - 13 тестов
- `AuthViewModelTest.kt` - 4 теста
- `LoginUiStateTest.kt` - 8 тестов
- `LoginCredentialsTest.kt` - 14 тестов
- `LoginUseCaseTest.kt` - 3 теста
- `ResetPasswordUseCaseTest.kt` - 3 теста
- `SWRepositoryAuthTest.kt` - 14 тестов
- `AuthInterceptorTest.kt` - 6 тестов
- `LoginSuccessTest.kt` - 3 теста

**UI-тесты (13 тестов):**
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
- Проверка типа ошибки выполняется в `HandleLoginUiState` для `LoginUiState.LoginError` и `LoginUiState.ResetError`
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

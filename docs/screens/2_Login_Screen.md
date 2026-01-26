# Реализация экрана авторизации (LoginScreen)

## Обзор

Экран авторизации позволяет пользователю войти в приложение workout.su или восстановить пароль от учетной записи.

**Источник истины для UI:** `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/LoginScreen.swift`

## Проверка реализации авторизации

### ✅ Сохранение токена авторизации

- **Реализовано:** `LoginUseCase` сохраняет токен в `SecureTokenRepository` при успешной авторизации
- **Тесты:** Есть unit-тесты в `LoginUseCaseTest.kt` (3 теста проверяют сохранение токена)
- **Статус:** ✅ Работает корректно

### ❌ Сохранение флага авторизации

- **Проблема:** Флаг авторизации (`isAuthorized`) НЕ сохраняется в `UserPreferencesRepository` при успешной авторизации
- **Причина:** `LoginUseCase` вызывает `swRepository.login(null)`, а `savePreference(true)` вызывается только если `token != null`
- **Статус:** ❌ Требует исправления (баг 5.4 - КРИТИЧНО!)

### ✅ Сброс флага авторизации при выходе

- **Реализовано:** `LogoutUseCase` вызывает `swRepository.forceLogout()`, который вызывает `savePreference(false)`
- **Статус:** ✅ Работает корректно

---

## Реализованные компоненты (готовы к использованию)

### Data Layer

- ✅ `SWRepository.resetPassword(login: String): Result<Unit>` - восстановление пароля
- ✅ `SecureTokenRepository` - безопасное хранение токена
  - ✅ Токен сохраняется при успешной авторизации через `LoginUseCase`
  - ✅ Токен очищается при выходе через `LogoutUseCase`
- ✅ `UserPreferencesRepository` - хранение флага авторизации
  - ✅ Флаг авторизации сбрасывается при выходе через `LogoutUseCase.forceLogout()`
  - ❌ **Флаг авторизации НЕ сохраняется** при успешной авторизации (баг 5.4)
- ✅ `AuthInterceptor` - перехватчик ошибок авторизации (401)
- ✅ `TokenInterceptor` - добавление токена в заголовки

### Domain Layer

- ✅ `LoginUseCase` - вход в систему
  - ✅ Сохраняет токен в `SecureTokenRepository` при успешной авторизации
  - ✅ Есть unit-тесты (LoginUseCaseTest.kt - 3 теста)
  - ❌ **НЕ сохраняет флаг авторизации** в `UserPreferencesRepository` (баг 5.4)
- ✅ `LogoutUseCase` - выход из системы
  - ✅ Очищает токен в `SecureTokenRepository`
  - ✅ Сбрасывает флаг авторизации через `SWRepository.forceLogout()`
- ✅ `AuthViewModel` - базовый ViewModel (нужно расширить)

### UI Components (Design System)

- ✅ `SWTextField` - текстовые поля с поддержкой ошибок (supportingText)
- ✅ `SWButton` - кнопки (LARGE для основной кнопки, SMALL для текстовых кнопок)
- ✅ `LoadingOverlayView` - оверлей загрузки

### Navigation

- ✅ Централизованная система маршрутов навигации

---

## Выполненные этапы

### Этап 1: Domain Layer ✅

Реализованы LoginCredentials (валидация), ResetPasswordUseCase, 15 тестов

### Этап 2: Локализация ✅

Все строки для LoginScreen (плейсхолдеры, кнопки, ошибки, алерты)

### Этап 3.1: LoginUiState ✅

Sealed состояния (Idle, Loading, LoginSuccess, ResetSuccess, LoginError, ResetError)

### Этап 3.2: LoginViewModel ✅

ViewModel с состояниями UI, учетными данными и методами (login, resetPassword), 14 тестов

### Этап 3.3: LoginScreen ✅

Полноценный экран авторизации с компонентами дизайн-системы, алертами и обработкой всех состояний

### Этап 4: Навигация и интеграция ✅

**Реализовано:**
- ✅ Маршрут LoginScreen в навигации (Destinations.kt:116)
- ✅ AppContainer для создания LoginViewModel
- ✅ LoginScreen.kt полностью реализован с компонентами дизайн-системы
- ✅ UserProfileCardView.kt реализован
- ✅ **4.1:** LoginScreen обновлен для модального окна (onDismiss, onLoginSuccess, LoginModalAppBar)
- ✅ **4.2:** LoginScreen интегрирован в RootScreen (DefaultAppContainer, currentUser, модальное окно)
- ✅ **4.3:** ProfileRootScreen обновлен (параметр user, IncognitoProfileView, UserProfileCardView, LogoutButton, calculateAge/getCountryName/getCityName, LogoutUseCase, обработка выхода, локализация "logout")
- ✅ **4.4:** Метод loginAndLoadUserData() в LoginViewModel с авторизацией, сохранением токена, загрузкой данных, ошибками сети, логированием
- ✅ **4.5:** Оверлей загрузки, кнопка закрытия отключена, логирование, обработка ошибок сети

**Осталось:**
- ❌ Блокировать жест "назад" при загрузке (опционально, не критично)

---

## Тестирование

### Unit-тесты (TDD)

**Выполнено (29 тестов):**
1. **LoginCredentialsTest.kt** ✅ - 12 тестов
2. **ResetPasswordUseCaseTest.kt** ✅ - 3 теста
3. **LoginViewModelTest.kt** ✅ - 14 тестов

### UI-тесты (опционально) ❌

- ❌ `LoginScreenTest.kt` - тестирование UI экрана авторизации

---

## Порядок реализации (TDD)

1-19. **TDD цикл и базовая реализация (LoginCredentials, ResetPasswordUseCase, LoginViewModel, LoginScreen, навигация, AppContainer, этап 4.1-4.5, локализация "logout", баг 5.1, баг 5.2.1)** ✅
20. **Исправить баг 5.2.2: Повторная авторизация после выхода (ошибка 502)** ✅
21. **Исправить баг 5.2.3: Отобразить ошибку восстановления пароля в UI** ✅
22. **Исправить баг 5.4: Сохранение флага авторизации при успешной авторизации** ❌ (КРИТИЧНО! Флаг авторизации не сохраняется)
23. **Создать UI-тесты для LoginScreen (LoginScreenTest.kt)** ❌ (ОБЯЗАТЕЛЬНО! Файл не создан, зависимости добавлены)
24. **Исправить баг 5.3: Реализовать LoginScreen как полноэкранный модальный лист** ❌
25. **Блокировка жеста "назад" при загрузке** ❌ (опционально, если потребуется после исправления бага 5.3)
26. Реализовать получение названий стран и городов из репозитория или кэша ❌
27. Проверить работу на устройстве/эмуляторе ❌

---

## Замечания и детали

### 1. Безопасное разворачивание опционалов

- Всегда использовать `?`, `?:`, `checkNotNull` вместо `!!`
- Логировать ошибки корректно

### 2. Логирование

- Все логи на русском языке
- Использовать уровни: Error, Info, Debug

### 3. Обработка ошибок сети

- Проверять наличие интернет-соединения перед запросом
- Показывать понятное сообщение об ошибке пользователю
- Использовать Result<T> для обработки ошибок в use cases

### 4. Форматирование

- Выполнять `make format` после изменений
- Соблюдать стиль кода проекта

### 5. Совместимость с iOS

- Расположение элементов соответствует iOS LoginScreen.swift
- Логика валидации соответствует iOS LoginCredentials.swift
- Алерты и сообщения соответствуют iOS Localizable.xcstrings
- Минимальная длина пароля: 6 символов (как в iOS)

---

## Необходимые строки локализации

Для завершения реализации этапа 4 нужно добавить следующие строки в локализацию:

### Строки для модального окна LoginScreen ✅ УЖЕ ЕСТЬ

```xml
<!-- app/src/main/res/values/strings.xml -->
<!-- УЖЕ ЕСТЬ (строка 170): -->
<!-- <string name="close">Close</string> -->

<!-- app/src/main/res/values-ru/strings.xml -->
<!-- УЖЕ ЕСТЬ: -->
<!-- <string name="close">Закрыть</string> -->
```

### Строки для ProfileRootScreen (LogoutButton) ❌ НУЖНО ДОБАВИТЬ

Добавить следующие строки в файлы локализации:

```xml
<!-- app/src/main/res/values/strings.xml -->
<string name="logout">Logout</string>

<!-- app/src/main/res/values-ru/strings.xml -->
<string name="logout">Выйти</string>
```

---

## Ссылки на референсы

### iOS (источник истины)

- `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/LoginScreen.swift` - экран авторизации
  - Строки 112-117: Запрос данных пользователя после успешной авторизации
  - Строка 35: `.interactiveDismissDisabled(isLoading)` - блокировка закрытия при загрузке
- `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Libraries/SWModels/Sources/SWModels/Auth/LoginCredentials.swift` - модель учетных данных
- `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Libraries/SWModels/Tests/SWModelsTest/LoginCredentialsTests.swift` - тесты модели
- `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Libraries/SWModels/Sources/SWModels/Resources/Localizable.xcstrings` - локализация

### Android (готовые компоненты)

- `app/src/main/java/com/swparks/ui/ds/SWTextField.kt` - текстовые поля
- `app/src/main/java/com/swparks/ui/ds/SWButton.kt` - кнопки
- `app/src/main/java/com/swparks/ui/ds/IncognitoProfileView.kt` - компонент для неавторизованного профиля
- `app/src/main/java/com/swparks/ui/ds/UserProfileCardView.kt` - компонент карточки профиля
- `app/src/main/java/com/swparks/ui/viewmodel/AuthViewModel.kt` - базовый ViewModel
- `app/src/main/java/com/swparks/domain/usecase/LoginUseCase.kt` - use case для входа
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt` - репозиторий

### Android (референсы для модальных окон)

- `app/src/main/java/com/swparks/ui/screens/more/MoreScreen.kt` - пример навигации между экранами
- `app/src/main/java/com/swparks/navigation/AppState.kt` - управление навигацией
- `app/src/main/java/com/swparks/navigation/TopLevelDestinations.kt` - верхнеуровневые назначения

### Документация проекта

- `docs/development-plan.md` - общий план разработки
- `docs/auth-token-plan.md` - план безопасной работы с токеном
- `docs/api-implementation-plan.md` - план реализации API
- `.cursor/skills/jetpack-compose-safe-areas/SKILL.md` - обработка безопасных зон в Compose

---

**Статус:** 🟢 ЭТАП 1 ВЫПОЛНЕН ✅ | ЭТАП 2 ВЫПОЛНЕН ✅ | ЭТАП 3 ВЫПОЛНЕН ✅ | ЭТАП 4 ВЫПОЛНЕН ✅ (100%) | ЭТАП 5 (ИСПРАВЛЕНИЕ БАГОВ И ТЕСТЫ) 🔄 (67% - добавлен критический баг 5.4)

**Найденные баги и задачи:**
- ✅ **Баг 5.1:** Заголовок в TopAppBar не выравнивается по центру ✅
- ✅ **Баг 5.2.1:** Невозможно ввести текст в текстовые поля LoginScreen ✅
- ✅ **Баг 5.2.2:** Невозможно авторизоваться повторно после выхода (ошибка 502 при загрузке данных) ✅
- ✅ **Баг 5.2.3:** Ошибка восстановления пароля не отображается в UI ✅
- ❌ **Баг 5.4:** Флаг авторизации не сохраняется при успешной авторизации (КРИТИЧНО!)
- ❌ **Задача 5.2.4:** Создать UI-тесты для LoginScreen (критическое упущение!)
- ❌ **Баг 5.3:** LoginScreen открывается не в модальном окне

**Выполнено:**
- ✅ TDD цикл и базовая реализация (LoginCredentials 12 тестов, ResetPasswordUseCase 3 теста, локализация, LoginUiState, LoginViewModel 14 тестов, LoginScreen, маршрут LoginScreen, AppContainer)
- ✅ Этап 4 (Навигация и интеграция) - полностью выполнен
- ✅ UserProfileCardView реализован в дизайн-системе
- ✅ Локализация для строки "logout" / "Выйти"
- ✅ Баг 5.1: Выравнивание заголовка в TopAppBar по центру
- ✅ Баг 5.2.1: Ввод текста в текстовые поля LoginScreen (коммит 8e3a621f)
- ✅ Баг 5.2.2: Повторная авторизация после выхода (ошибка 502)
- ✅ Баг 5.2.3: Отображение ошибки восстановления пароля в UI
- ✅ Сохранение токена в SecureTokenRepository при успешной авторизации (есть тесты)

**Осталось:**
- ❌ Баг 5.4: Сохранение флага авторизации при успешной авторизации (КРИТИЧНО!)
- ❌ Задача 5.2.4: Создать UI-тесты для LoginScreen (критическое упущение!)
- ❌ Баг 5.3: Реализовать LoginScreen как полноэкранный модальный лист
- ❌ Блокировка жеста "назад" при загрузке (опционально, если потребуется после исправления бага 5.3)
- ❌ Реализовать получение названий стран и городов из репозитория или кэша
- ❌ Проверить работу на устройстве/эмуляторе

**Следующий шаг:** Исправление бага 5.4 (сохранение флага авторизации при успешной авторизации) - КРИТИЧНО!

---

## Исправление багов UI

### Баг 5.1: Заголовок в TopAppBar не выравнивается по центру ✅

**Статус:** ✅ ИСПРАВЛЕНО

**Исправлено:** Заменен `TopAppBar` на `CenterAlignedTopAppBar` в функции `LoginModalAppBar` для автоматического центрирования заголовка.

**Файл:** `app/src/main/java/com/swparks/ui/screens/auth/LoginScreen.kt` (строка 167)

---

### Баг 5.2: Проблемы с вводом текста и повторной авторизацией

#### 5.2.1: Невозможно ввести текст в текстовые поля LoginScreen

**Статус:** ✅ ИСПРАВЛЕНО

**Описание:**
На экране LoginScreen можно установить курсор в текстовые поля (LoginField и PasswordField), но невозможно ввести текст. Причина в том, что при вводе текста вызывался `credentials = credentials.copy(...)` в методах `onLoginChange` и `onPasswordChange`, что создавало новый объект `LoginCredentials`, но не вызывало рекомпозицию в LoginScreen, потому что Compose не отслеживал изменения внутри объекта.

**Исправлено:** Заменен подход с `var credentials` на отдельные `MutableState` для каждого поля (`_login` и `_password`). Теперь свойство `credentials` является вычисляемым (val) getter, который собирает логин и пароль из состояний. Это корректно вызывает рекомпозицию при изменении текста.

**Коммит:** 8e3a621f3e1ceb2e3ef0eb50f2d9442f537950f8

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/LoginViewModel.kt`

---

#### 5.2.2: Невозможно авторизоваться повторно после выхода ✅

**Статус:** ✅ ИСПРАВЛЕНО

**Симптомы:**
Первая авторизация проходит успешно, выход выполняется, но при повторной авторизации возникает ошибка 502 при загрузке данных пользователя.

**Причина:** Сервер возвращает ошибку 502 Bad Gateway вместо JSON ответа при запросе социальных обновлений после повторной авторизации.

**Исправлено:** Внедрена retry логика в `LoginViewModel.loadUserDataWithRetry()` для обработки временных ошибок сервера (502, 503, 504, IOException).

**Результат:**
- ✅ Повторная авторизация работает корректно
- ✅ Данные пользователя загружаются успешно
- ✅ Профиль отображает нужную информацию

---

#### 5.2.3: Ошибка восстановления пароля не отображается в UI ✅

**Статус:** ✅ ИСПРАВЛЕНО

**Описание:**
При попытке восстановить пароль сервер возвращает ошибку (например, 400 Bad Request), но эта ошибка логируется в консоль и не отображается пользователю в UI. По аналогии с ошибкой авторизации (которая отображается под полем с паролем), ошибка восстановления пароля должна отображаться под полем для логина.

**Причина:**
В `HandleLoginUiState` при состоянии `ResetError` вызывался `onResetError()`, который очищал ошибку через `viewModel.clearErrors()`. Это приводило к тому, что ошибка исчезала из состояния UI, прежде чем успевала отобразиться пользователю.

**Исправлено:**
В `HandleLoginUiState` при обработке состояния `ResetError` (строки 419-423) ошибка больше не очищается через `onResetError()`. Теперь ошибка сохраняется в `resetErrorState` и отображается под полем логина через `supportingText` в `LoginField` (строка 241). Ошибка очищается только при вводе новых данных (через `onLoginChange`).

**Файл:** `app/src/main/java/com/swparks/ui/screens/auth/LoginScreen.kt` (строки 419-423)

**Приоритет:** Средний (небольшая доработка UX для улучшения обратной связи с пользователем)

**Связанные разделы:** Раздел 4.5 (Обработка ошибок сети в оверлее загрузки), Баг 5.2.1 (Отображение ошибок авторизации), Задача 5.2.4 (Создание UI-тестов)

---

### Баг 5.2.4: UI-тесты для LoginScreen не созданы

**Статус:** ❌ НЕ ВЫПОЛНЕНО (КРИТИЧЕСКОЕ УПУЩЕНИЕ!)

---

### Баг 5.4: Флаг авторизации не сохраняется при успешной авторизации

**Статус:** ❌ НЕ ИСПРАВЛЕНО (КРИТИЧНО!)

**Описание:**
При успешной авторизации через `LoginUseCase` токен сохраняется в `SecureTokenRepository`, но флаг авторизации (`isAuthorized`) не сохраняется в `UserPreferencesRepository`. Это происходит потому, что `LoginUseCase` вызывает `swRepository.login(null)`, а в `SWRepository.login()` метод `savePreference(true)` вызывается только если `token != null`.

**Текущая реализация:**

**LoginUseCase.kt (строка 46):**

```kotlin
return swRepository.login(null)  // ❌ Передается null вместо токена
```

**SWRepository.kt (строки 240-241):**

```kotlin
if (token != null) {  // ❌ Условие не выполняется, так как token == null
    savePreference(true)
}
```

**Проблема:**
- Токен сохраняется в `SecureTokenRepository` ✅
- Флаг авторизации НЕ сохраняется в `UserPreferencesRepository` ❌
- При выходе флаг корректно сбрасывается через `LogoutUseCase.forceLogout()` ✅

**Последствия:**
- Приложение не знает, что пользователь авторизован
- Может нарушиться логика проверки авторизации в других частях приложения
- Поток `isAuthorized` может показывать неверное состояние

**Файлы:**
- `app/src/main/java/com/swparks/domain/usecase/LoginUseCase.kt` (строка 46) - нужно исправить
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt` (строки 236-252) - текущая реализация login()
- `app/src/test/java/com/swparks/domain/usecase/LoginUseCaseTest.kt` - нужно добавить тест

**Пошаговый план исправления:**

1. **Исправить LoginUseCase для сохранения флага авторизации:**

   Передавать токен в `swRepository.login(token)` и убрать проверку `if (token != null)`:

   ```kotlin
   override suspend operator fun invoke(credentials: LoginCredentials): Result<LoginSuccess> {
       val token = tokenEncoder.encode(credentials)
       secureTokenRepository.saveAuthToken(token)
       return swRepository.login(token)  // Передаем токен вместо null
   }
   ```

   И в `SWRepository.login()` убрать проверку:

   ```kotlin
   override suspend fun login(token: String?): Result<LoginSuccess> =
       try {
           val response = swApi.login()
           // Сохраняем флаг авторизации при успешном входе
           savePreference(true)  // Убрать проверку if (token != null)
           Result.success(response)
       } catch (e: IOException) {
           // ...
       }
   ```

2. **Добавить тест для проверки сохранения флага авторизации:**

   В `LoginUseCaseTest.kt` добавить тест:

   ```kotlin
   @Test
   fun invoke_whenSuccessfulLogin_thenSavesAuthorizationFlag() = runTest {
       // Given
       coEvery { tokenEncoder.encode(testCredentials) } returns testToken
       coEvery { swRepository.login(testToken) } returns Result.success(LoginSuccess(testUserId))
       
       // When
       val result = loginUseCase(testCredentials)
       
       // Then
       assertTrue(result.isSuccess)
       coVerify(exactly = 1) { swRepository.login(testToken) }
   }
   ```

3. **Проверить существующие тесты:**
   - Убедиться, что все существующие тесты в `LoginUseCaseTest.kt` проходят
   - Обновить моки при необходимости

4. **Проверить интеграцию с LogoutUseCase:**
   - Убедиться, что `LogoutUseCase` корректно сбрасывает флаг через `forceLogout()`
   - Проверить, что флаг авторизации корректно обновляется при выходе

5. **Тестирование:**
   - Выполнить авторизацию через LoginScreen
   - Проверить, что флаг `isAuthorized` становится `true` в `UserPreferencesRepository`
   - Выполнить выход из учетной записи
   - Проверить, что флаг `isAuthorized` становится `false`

**Приоритет:** ВЫСОКИЙ (критично для корректной работы авторизации)

**Связанные разделы:** Этап 1 (Domain Layer), раздел "Тестирование" (Unit-тесты), LogoutUseCase (сброс флага авторизации)

---

## 11. **UI-тесты для авторизации**

### Общая информация

**Статус:**
- ✅ Зависимости добавлены в `app/build.gradle.kts` (androidTestImplementation для ui.test.manifest, mockk, retrofit)
- ✅ Референсы: MoreScreenTest.kt (10 тестов), ThemeIconScreenTest.kt (17 тестов)
- ❌ Папка `auth/` для UI-тестов НЕ создана (упущение!)
- ❌ Файл `LoginScreenTest.kt` НЕ создан (упущение!)
- ❌ Тесты для ошибок, авторизации и восстановления пароля НЕ реализованы

**Почему упущение:** Зависимости добавлены при реализации этапа 5.2.2, но файл с тестами не создан

### Что нужно реализовать

Создать папку `app/src/androidTest/java/com/swparks/ui/screens/auth/` и файл `LoginScreenTest.kt` с тестами для LoginScreen.

## 12. **Реализованные изменения (этап 5.2.2)**

### 12.1. Добавлена retry логика для обработки временных ошибок сервера ✅

**Файл:** `app/src/main/java/com/swparks/ui/viewmodel/LoginViewModel.kt`

**Что реализовано:**
- Приватный метод `loadUserDataWithRetry(userId: Long)` (строки 175-227) с retry до 3 раз для ошибок 502, 503, 504, IOException
- Константы: HTTP_BAD_GATEWAY=502, HTTP_SERVICE_UNAVAILABLE=503, HTTP_GATEWAY_TIMEOUT=504, MAX_RETRIES=3, RETRY_DELAY_MS=1000L
- Метод `loginAndLoadUserData()` вызывает `loadUserDataWithRetry(userId)` с улучшенным логированием

**Результат:**
- ✅ Retry логика реализована и протестирована в unit-тестах
- ✅ Все unit-тесты проходят успешно (`./gradlew test` → BUILD SUCCESSFUL)
- ✅ Код соответствует стандартам проекта

---

## 13. **План дальнейшей работы**

1. **Создать UI-тесты для LoginScreen** (критическое упущение!):
   - Создать папку `app/src/androidTest/java/com/swparks/ui/screens/auth/`
   - Создать файл `LoginScreenTest.kt` с тестами для отображения элементов, ошибок авторизации и восстановления пароля

2. **Улучшение логирования:** детальное логирование retry попыток, метрики, отправка логов на сервер

3. **Дополнительные сценарии тестирования:** плохое соединение, истекший токен, неверные данные

### Баг 5.3: LoginScreen открывается не в модальном окне

**Статус:** ❌ НЕ ИСПРАВЛЕНО

**Описание:**
Экран LoginScreen открывается как обычный экран навигации через `navController.navigate`, с которого можно вернуться назад (свайп, кнопка "Назад", BottomNavigationBar виден). Нужно реализовать как полноэкранный модальный лист (full-screen dialog), который перекрывает BottomNavigationBar и блокирует закрытие жестом "назад".

**Файлы:**
- `app/src/main/java/com/swparks/ui/screens/RootScreen.kt` (строки 206-224) - текущая реализация через обычную навигацию
- `app/src/main/java/com/swparks/ui/screens/auth/LoginScreen.kt` - сама реализация экрана
- `SwiftUI-WorkoutApp/SwiftUI-WorkoutApp/Screens/Common/LoginScreen.swift` - iOS-референс модального окна (.sheet или fullScreenCover)

**План исправления:**
1. Проверить текущую реализацию в RootScreen (обычная навигация через `navController.navigate`)
2. Изучить iOS-реализацию модального окна (.sheet или fullScreenCover)
3. Выбрать подход: Full-screen Dialog (рекомендуется) или BottomSheetScaffold из Material 3
4. Реализовать полноэкранный Dialog в RootScreen:
   - Добавить состояние `showLoginModal`
   - Изменить навигацию на IncognitoProfileView (открывает модальное окно)
   - Добавить Dialog с LoginScreen (блокирует закрытие кнопкой "назад" и кликом вне)
5. Удалить маршрут LoginScreen из NavHost (если используется Dialog)
6. Тестирование: модальное окно открывается/закрывается корректно, BottomNavigationBar скрыт, свайп не работает

**Приоритет:** Низкий (UX улучшение, функционал работает)

**Связанные разделы:** Этап 4 (Навигация и интеграция), раздел 6 (Модальные окна в iOS)

---

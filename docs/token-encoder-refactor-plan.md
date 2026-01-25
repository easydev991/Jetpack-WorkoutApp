# Рефакторинг: Перенос генерации токена в Data Layer

## Цель

Перенести генерацию токена авторизации из `LoginCredentials` (Domain Layer) в отдельный класс в Data Layer для устранения зависимости от Android SDK в Domain Layer и улучшения чистоты архитектуры.

**Текущая проблема:**
- `LoginCredentials.token` использует `android.util.Base64` (Android SDK класс)
- Domain layer не должен зависеть от платформо-специфичных классов
- Unit-тесты требуют настройку `isReturnDefaultValues = true` для работы

**Решение:**
- Создать `TokenEncoder` в data layer
- Перенести логику генерации токена из `LoginCredentials` в `TokenEncoder`
- Обновить `LoginUseCase` для использования `TokenEncoder`
- Удалить свойство `token` из `LoginCredentials`

---

## Этап 1: Создание TokenEncoder в Data Layer

### 1.1 Создать класс TokenEncoder ✅

- **Файл:** `app/src/main/java/com/swparks/data/TokenEncoder.kt`
- **Назначение:** Генерация Base64 токена из учетных данных пользователя
- **Логика:**
  - Принимает `LoginCredentials` на вход
  - Проверяет, что логин и пароль не пустые
  - Возвращает `String?` - токен или null
  - Использует `java.util.Base64` (кроссплатформенное решение, работает в тестах без mock)
- **Правила кода:**
  - Следовать стилю из `.cursor/rules/code-style.mdc`
  - Логи на русском языке
  - KDoc для публичного метода

### 1.2 Создать модульные тесты для TokenEncoder ✅

- **Файл:** `app/src/test/java/com/swparks/data/TokenEncoderTest.kt`
- **Тестовые сценарии:**
  - `encode_whenValidCredentials_thenReturnsBase64Token` - валидные данные → токен
  - `encode_whenEmptyLogin_thenReturnsNull` - пустой логин → null
  - `encode_whenEmptyPassword_thenReturnsNull` - пустой пароль → null
  - `encode_whenWhitespaceOnlyCredentials_thenReturnsNull` - только пробелы → null
  - `encode_whenTrimmedCredentials_thenReturnsCorrectToken` - обрезка пробелов
- **Правила тестирования:**
  - Использовать MockK для моков
  - Формат именования: `funName_whenCondition_thenExpectedResult`
  - Использовать `runTest` для suspend тестов
  - Не использовать wildcard imports (исправлено на `assertEquals`, `assertNotNull`, `assertNull`)

**Критерий завершения:** Все тесты для `TokenEncoder` проходят успешно ✅

---

## Этап 2: Обновление Domain Layer

### 2.1 Удалить свойство token из LoginCredentials ✅

- **Файл:** `app/src/main/java/com/swparks/model/LoginCredentials.kt`
- **Изменения:**
  - Удалить свойство `token: String?` (строки 25-39)
  - Удалить импорт `android.util.Base64`
  - Оставить остальные свойства: `isReady`, `canRestorePassword`, `canLogIn()`

### 2.2 Проверить использование credentials.token в проекте ✅

- **Действие:** Найти все использования `credentials.token` или `*.token`
- **Команда:** `rg "\.token" --type kotlin` или поиск через IDE
- **Анализ:**
  - В `LoginUseCaseTest.kt` - использовано в тесте (заменено на `tokenEncoder.encode(any())`)
  - В `LoginViewModel` - использование `Log.i`, `Log.e` (решено через Logger)

### 2.3 Обновить модульные тесты для LoginCredentials ✅

- **Файлы:** `LoginCredentials` тесты (если есть)
- **Изменения:**
  - Удалить тесты, которые проверяют свойство `token`
  - Обновить тесты, если они зависят от токена
- **Примечание:** Для `LoginCredentials` тесты не нужны, так как остались только простые свойства без сложной логики

**Критерий завершения:** Свойство `token` удалено из `LoginCredentials`, все использования найдены и зафиксированы ✅

---

## Этап 3: Обновление Data Layer

### 3.1 Обновить LoginUseCase для использования TokenEncoder ✅

- **Файл:** `app/src/main/java/com/swparks/domain/usecase/LoginUseCase.kt`
- **Изменения:**
  - Добавить параметр `tokenEncoder: TokenEncoder` в конструктор
  - Заменить `credentials.token` на `tokenEncoder.encode(credentials)` в методе `invoke()`
  - Обновить KDoc для класса
- **До:**

  ```kotlin
  class LoginUseCase(
      private val secureTokenRepository: SecureTokenRepository,
      private val swRepository: SWRepository
  )
  override suspend operator fun invoke(credentials: LoginCredentials): Result<LoginSuccess> {
      val token = credentials.token
      secureTokenRepository.saveAuthToken(token)
      return swRepository.login(null)
  }
  ```

- **После:**

  ```kotlin
  class LoginUseCase(
      private val tokenEncoder: TokenEncoder,
      private val secureTokenRepository: SecureTokenRepository,
      private val swRepository: SWRepository
  )
  override suspend operator fun invoke(credentials: LoginCredentials): Result<LoginSuccess> {
      val token = tokenEncoder.encode(credentials)
      secureTokenRepository.saveAuthToken(token)
      return swRepository.login(null)
  }
  ```

### 3.2 Обновить DI контейнер (AppContainer) ✅

- **Файл:** `app/src/main/java/com/swparks/data/AppContainer.kt`
- **Изменения:**
  - Добавить создание `TokenEncoder` instance
  - Обновить создание `LoginUseCase` с передачей `tokenEncoder`
- **Пример:**

  ```kotlin
  // Добавить
  private val tokenEncoder: TokenEncoder by lazy {
      TokenEncoder()
  }

  // Обновить
  override val loginUseCase: ILoginUseCase by lazy {
      LoginUseCase(
          tokenEncoder = tokenEncoder,
          secureTokenRepository = secureTokenRepository,
          swRepository = swRepository
      )
  }
  ```

### 3.3 Обновить модульные тесты для LoginUseCase ✅

- **Файл:** `app/src/test/java/com/swparks/domain/usecase/LoginUseCaseTest.kt`
- **Изменения:**
  - Добавить `private lateinit var tokenEncoder: TokenEncoder` в `setup()`
  - Создать mock: `tokenEncoder = mockk(relaxed = true)` или `mockk()` с настройками
  - Передать `tokenEncoder` в конструктор `LoginUseCase`
  - Заменить проверку `testCredentials.token` на `tokenEncoder.encode(any())` или ожидаемое значение
  - Добавить проверку `coVerify(exactly = 1) { tokenEncoder.encode(testCredentials) }`
- **Пример:**

  ```kotlin
  @Before
  fun setup() {
      tokenEncoder = mockk(relaxed = true)
      secureTokenRepository = mockk(relaxed = true)
      swRepository = mockk(relaxed = true)
      loginUseCase = LoginUseCase(tokenEncoder, secureTokenRepository, swRepository)
  }

  @Test
  fun invoke_whenValidCredentials_thenSavesTokenAndCallsLogin() = runTest {
      // Given
      coEvery { swRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))
      coEvery { tokenEncoder.encode(any()) } returns "test_token_123"

      // When
      val result = loginUseCase(testCredentials)

      // Then
      assertTrue(result.isSuccess)
      assertEquals(testUserId, result.getOrNull()?.userId)
      coVerify(exactly = 1) { secureTokenRepository.saveAuthToken("test_token_123") }
      coVerify(exactly = 1) { swRepository.login(null) }
      coVerify(exactly = 1) { tokenEncoder.encode(testCredentials) }
  }
  ```

**Критерий завершения:** `LoginUseCase` использует `TokenEncoder`, DI контейнер обновлен, тесты проходят ✅

---

## Этап 4: Обновление UI Layer

### 4.1 Обновить LoginViewModel тесты ✅

- **Файл:** `app/src/test/java/com/swparks/ui/viewmodel/LoginViewModelTest.kt`
- **Проверка:** Используются ли `credentials.token` в тестах
- **Изменения:**
  - Не использовать напрямую `credentials.token`
  - Убедиться, что тесты не зависят напрямую от генерации токена

### 4.2 Проверить использование credentials.token в UI компонентах ✅

- **Действие:** Поиск в `app/src/main/java/com/swparks/ui/`
- **Анализ:** Если найдены использования - заменить на соответствующие вызовы через `LoginUseCase`
- **Примечание:** Обычно `LoginViewModel` не должен напрямую обращаться к `credentials.token` - логика должна быть в Use Case

**Критерий завершения:** UI layer не использует напрямую `credentials.token`, все тесты проходят ✅

---

## Дополнительный этап: Реализация Logger для решения проблемы с android.util.Log ✅

### Реализация Logger интерфейса ✅

**Проблема:** `LoginViewModel` использует прямые вызовы `android.util.Log`, которые не замоканы в unit-тестах, что приводит к падению 3 тестов.

**Решение:** Создать абстрактный интерфейс `Logger` и реализации (Android, NoOp) для тестов.

**Созданные файлы:**
1. `app/src/main/java/com/swparks/util/Logger.kt` - интерфейс для логирования
   - Методы: `d()`, `w()`, `e()`, `i()`
   - Позволяет мокировать логирование в тестах

2. `app/src/main/java/com/swparks/util/AndroidLogger.kt` - реализация для Android
   - Использует `android.util.Log` для реального логирования
   - Реализует интерфейс `Logger`

3. `app/src/test/java/com/swparks/util/NoOpLogger.kt` - пустая реализация для тестов
   - Не выполняет никаких действий
   - Позволяет тестам проходить без необходимости мокать `android.util.Log`

**Измененные файлы:**
1. `app/src/main/java/com/swparks/ui/viewmodel/LoginViewModel.kt`
   - Заменены прямые вызовы `android.util.Log` на интерфейс `Logger`
   - Добавлен параметр `logger: Logger` в конструктор
   - Использованы методы: `logger.i()`, `logger.e()`

2. `app/src/main/java/com/swparks/JetpackWorkoutApplication.kt`
   - Добавлено свойство `val logger: Logger = AndroidLogger()`
   - Используется для внедрения в ViewModels через Factory

3. `app/src/test/java/com/swparks/ui/viewmodel/LoginViewModelTest.kt`
   - Используется `NoOpLogger` в тестах вместо мокирования

**Результат:** Все 18 тестов `LoginViewModelTest` теперь проходят успешно ✅

**Критерий завершения:** UI layer не использует напрямую `credentials.token`, все тесты проходят, проблема с `android.util.Log` решена ✅

---

## Этап 5: Убрать isReturnDefaultValues из build.gradle.kts ✅

### 5.1 Удалить временную настройку ✅

- **Файл:** `app/build.gradle.kts`
- **Изменения:**
  - Удалить блок `testOptions` с `isReturnDefaultValues = true` (если был)
  - Примечание: Настройка уже отсутствует в файле
- **Проверка:** Убедиться, что все тесты все еще проходят без этой настройки

### 5.2 Запустить все тесты для проверки ✅

- **Команда:** `./gradlew testDebugUnitTest`
- **Ожидание:** Все тесты проходят (489 тестов, как было до этого с настройкой)

**Критерий завершения:** Настройка `isReturnDefaultValues = true` отсутствует, все тесты проходят ✅

---

## Этап 6: Финальная проверка и документация

### 6.1 Запустить полный тест-сьют ✅

- **Команда:** `./gradlew test` или `make test`
- **Ожидание:** Все тесты проходят, нет ошибок
- **Фактический результат:** 489 тестов проходят успешно ✅

### 6.2 Проверить качество кода ✅

- **Команды:**
  - `./gradlew ktlintCheck` - проверка стиля кода
  - `./gradlew detekt` - проверка качества кода
- **Ожидание:** Нет lint ошибок
- **Фактический результат:**
  - ktlint: успешно ✅
  - detekt: успешно (только существующие предупреждения, не связанные с рефакторингом) ✅

### 6.3 Применить форматирование ✅

- **Команда:** `./gradlew ktlintFormat` или `make format`
- **Проверка:** Код отформатирован согласно стандартам проекта
- **Фактический результат:** Код отформатирован ✅

### 6.4 Обновить документацию (если нужно) ✅

- **Файлы:** Проверить `.cursor/rules/architecture.mdc` и другие документацию
- **Изменения:** Если документация упоминает `credentials.token` - обновить
- **Анализ:** Документация не упоминает `credentials.token`, обновления не требуются
- **Примечание:** Рекомендуется добавить описание `TokenEncoder` и `Logger` в документацию

### 6.5 Убедиться, что приложение собирается ✅

- **Команда:** `./gradlew assembleDebug` или `make build`
- **Ожидание:** Сборка успешна, нет ошибок
- **Фактический результат:** Сборка успешна ✅

**Критерий завершения:** Все тесты проходят (489), сборка успешна, качество кода в норме ✅

---

## Зависимости между этапами

```
Этап 1 (TokenEncoder) → Этап 2 (LoginCredentials) → Этап 3 (LoginUseCase) → Этап Logger → Этап 5 (build.gradle) → Этап 6 (Проверка)
```

**Пояснение:**
- Сначала создаем `TokenEncoder` (новый функционал)
- Потом удаляем `token` из `LoginCredentials` (убираем старый код)
- Затем обновляем `LoginUseCase` (используем новый класс)
- Реализуем `Logger` для решения проблемы с `android.util.Log`
- Потом проверяем UI слой (на случай, если использовали старый API)
- Затем убираем временную настройку (проверка, что все работает)
- Финальная проверка всего проекта

---

## Потенциальные проблемы и решения

### Проблема 1: Credentials.token используется в других частях проекта ✅ РЕШЕНО

**Решение:** Найти все использования на Этапе 2.2 и заменить на вызов через `TokenEncoder` или `LoginUseCase`
- В `LoginUseCaseTest.kt` - заменено на `tokenEncoder.encode(any())`
- В `LoginViewModel` - не используется напрямую

### Проблема 2: Тесты падают после удаления isReturnDefaultValues ✅ РЕШЕНО

**Решение:** Реализован интерфейс `Logger` с `NoOpLogger` для тестов
- Падающие тесты в `LoginViewModelTest` (3 теста) были связаны с `android.util.Log`
- После внедрения `Logger` и `NoOpLogger` - все тесты проходят

### Проблема 3: LoginViewModel напрямую использует android.util.Log ✅ РЕШЕНО

**Решение:** Создан интерфейс `Logger` и внедрен через DI
- `LoginViewModel` использует `logger: Logger` вместо прямых вызовов `android.util.Log`
- В тестах используется `NoOpLogger` для отключения логирования

### Проблема 4: DI контейнер не найден или сложный ✅ РЕШЕНО

**Решение:** Найден `AppContainer.kt` и обновлен с добавлением `TokenEncoder`
- Добавлена lazy инициализация `TokenEncoder`
- Обновлен Factory для `LoginUseCase` с передачей `logger`

---

## Объем работы

- **Оценка времени:** 3 часа
- **Количество файлов для изменения:** ~11 файлов
- **Сложность:** Средняя (рефакторинг с сохранением функционала и решением проблемы с logger)

---

## Критерии успеха

✅ Свойство `token` удалено из `LoginCredentials`
✅ `TokenEncoder` создан и протестирован в data layer
✅ `LoginUseCase` использует `TokenEncoder`
✅ Реализован интерфейс `Logger` для решения проблемы с `android.util.Log`
✅ Все модульные тесты проходят (TokenEncoder: 7, LoginUseCase: 3, LoginViewModel: 18, итого: 28)
✅ Все 489 тестов проекта проходят успешно
✅ Настройка `isReturnDefaultValues = true` удалена (была отсутствует)
✅ Lint (ktlint) и detekt проходят без новых ошибок
✅ Приложение собирается успешно

---

## Следующие улучшения (опционально)

После завершения рефакторинга можно рассмотреть:

1. **Создать интерфейс ITokenEncoder** для улучшения тестируемости
2. **Добавить логирование** в `TokenEncoder` для отладки (токен генерируется, но может быть null)
3. **Рассмотреть использование java.util.Base64** вместо android.util.Base64 (уже сделано для максимальной переносимости)
4. **Добавить unit тесты для edge cases** в `TokenEncoder` (null credentials, unicode символы и т.д.)
5. **Создать документацию** по использованию `TokenEncoder` и `Logger` в `.cursor/rules/` если будет использоваться в других местах
6. **Рассмотреть использование Logger в других ViewModels** для консистентности логирования
7. **Добавить тесты** для других ViewModels, если они используют `android.util.Log` или `android.os.Bundle`

# План безопасной работы с токеном авторизации

## Обзор

Документ описывает реализацию безопасного хранения и использования токена авторизации в Android-приложении Jetpack-WorkoutApp с соблюдением консистентности с iOS-версией.

---

## Статус реализации

**Статус:** ✅ **ЗАВЕРШЕНО**

### Выполненные этапы

- ✅ **Этапы 0-16:** SWApi, Tink, EncryptedStringSerializer, CryptoManager, SecureTokenRepository, TokenInterceptor, AppContainer, LoginUseCase, LogoutUseCase, AuthViewModel (тесты + реализация)

---

## Проблема

**Баг #5:** Отсутствует безопасное хранение и использование токена авторизации. В текущей реализации:

- DataStore содержит только ключ `is_authorized` для флага авторизации
- Нет ключа для хранения токена авторизации
- Токен НЕ сохраняется локально для повторного использования в других запросах
- Токен НЕ добавляется в заголовки HTTP запросов автоматически

---

## Анализ iOS-реализации (референс для консистентности)

### Как работает авторизация в iOS

**Шаг 1: Авторизация (логин)**

```swift
// AuthClient.swift - токен передаётся явно в параметре
public func logIn(with token: String?) async throws -> Int {
    let endpoint = Endpoint.login
    let finalComponents = try await makeComponents(for: endpoint, with: token)
    let result: LoginResponse = try await service.requestData(components: finalComponents)
    return result.userId
}
```

**Шаг 2: Сохранение токена**

```swift
// SWClient.swift - токен сохраняется в authHelper
func makeComponents(for endpoint: Endpoint, with token: String? = nil) async throws -> RequestComponents {
    let savedToken = await authHelper.authToken // Из Keychain
    return .init(
        path: endpoint.urlPath,
        queryItems: endpoint.queryItems,
        httpMethod: endpoint.method,
        hasMultipartFormData: endpoint.hasMultipartFormData,
        bodyParts: endpoint.bodyParts,
        token: token ?? savedToken // Если не передан, берёт из authHelper
    )
}
```

**Шаг 3: Добавление токена в заголовок**

```swift
// RequestComponents.swift - токен добавляется в заголовок Authorization
if let token, !token.isEmpty {
    allHeaders.append(.init(key: "Authorization", value: "Basic \(token)"))
}
request.allHTTPHeaderFields = Dictionary(
    uniqueKeysWithValues: allHeaders.map { ($0.key, $0.value) }
)
```

**Ключевые моменты iOS-реализации:**

1. ✅ Токен ВСЕГДА в заголовке: `Authorization: Basic {token}`, если токен существует и не пустой
2. ✅ Авторизация тоже через заголовок: запрос login не содержит токена в теле, токен добавляется в заголовок
3. ✅ Последующие запросы: токен берётся из `authHelper.authToken` (хранится в Keychain)
4. ✅ Интерцептор аналог: OkHttp Interceptor для Android
5. ✅ Без токена: если токена нет (пользователь не авторизован), заголовок Authorization не добавляется
6. ✅ Защита от пустого токена: проверка `!token.isEmpty` перед добавлением заголовка

---

## Архитектура безопасности

**Стек технологий:**

```
┌─────────────────────────────────────┐
│           Android Keystore             │
│   (Хранение ключей шифрования)       │
└──────────────┬──────────────────────────┘
               │
               │ Управление ключами
               ▼
┌─────────────────────────────────────────────┐
│       CryptoManager (Tink)          │
│  - Aead шифрование                   │
│  - AES-128-GCM-HKDF               │
│  - Генерация/чтение keyset        │
└──────────────┬──────────────────────────┘
               │
               │ Шифрованные данные (Base64)
               ▼
┌─────────────────────────────────────────────┐
│   Preferences DataStore               │
│  - Асинхронное хранение             │
│  - stringPreferencesKey              │
│  - Custom Serializer с шифрованием   │
└─────────────────────────────────────────────┘
```

**Почему Preferences DataStore вместо Proto DataStore:**

1. ✅ Совместимость с AGP 9.0.0 - protobuf-gradle-plugin 0.9.6 ломает сборку на AGP 9.0+
2. ✅ Меньше зависимостей - только `datastore-preferences` + `tink-android`
3. ✅ Быстрая сборка - не требуется генерация кода через protoc
4. ✅ Простота - расширение существующего UserPreferencesRepository вместо новой схемы
5. ✅ Без миграций - добавление encrypted_token в существующие настройки

**Компоненты архитектуры:**

1. **CryptoManager** - Управление шифрованием
   - Создание ключа в Android Keystore (если не существует)
   - Хранение Tink keyset в SharedPreferences
   - Методы `encrypt(data: ByteArray): ByteArray` и `decrypt(ciphertext: ByteArray): ByteArray`
   - Использует `Aead` с шаблоном `AES_128_GCM_HKDF`

2. **SecureTokenRepository** - API для работы с защищенным токеном
   - Сохранение токена с шифрованием через Preferences DataStore
   - Чтение и расшифрование токена
   - Синхронное очищение для использования в Interceptor

3. **TokenInterceptor** - Добавление токена в заголовки
   - Получает токен из `SecureTokenRepository`
   - Добавляет заголовок `Authorization: Basic {token}` в запросы
   - Не добавляет заголовок если токена нет

4. **AuthInterceptor** - Обработка ошибок 401 (без изменений)
   - Сохраняется как есть
   - При 401 очищает флаг `isAuthorized` в UserPreferencesRepository
   - Опционально может вызывать `secureTokenRepository.clearAuthTokenSync()`

---

## План TDD реализации по шагам

### Этапы 0-13: SWApi, Tink, EncryptedStringSerializer, CryptoManager, SecureTokenRepository, TokenInterceptor, AppContainer, LoginUseCase ✅

**Реализовано:**

- **Этапы 0-5:** SWApi исправлен, зависимости Tink, EncryptedStringSerializer, CryptoManager (тесты + реализация)
- **Этапы 6-7:** SecureTokenRepository с шифрованием через Tink и кодированием в Base64 (тесты + реализация)
- **Этап 8:** Unit-тесты для TokenInterceptor (все кейсы)
- **Этап 9:** TokenInterceptor для добавления заголовка `Authorization: Basic {token}`
- **Этапы 10-11:** Создание компонентов в AppContainer и добавление интерцепторов в OkHttpClient
- **Этапы 12-13:** LoginUseCase сохраняет токен и вызывает login (тесты + реализация)

**Результат:** Все тесты проходят успешно, консистентно с iOS-реализацией.

---

### Этапы 14-16: LogoutUseCase, AuthViewModel ✅

**Реализовано:**

- **Этап 14:** Unit-тесты для LogoutUseCase (`LogoutUseCaseTest.kt`) ✅
- **Этап 15:** LogoutUseCase с очисткой токена и сбросом флага isAuthorized (`LogoutUseCase.kt`) ✅
- **Этап 16:** AuthViewModel с интерфейсами ILoginUseCase и ILogoutUseCase, обновление AppContainer (`AuthViewModel.kt`, `AuthViewModelTest.kt`) ✅

**Файлы:**

- `app/src/test/java/com/swparks/domain/usecase/LogoutUseCaseTest.kt` - Unit тесты для LogoutUseCase
- `app/src/main/java/com/swparks/domain/usecase/LogoutUseCase.kt` - Реализация LogoutUseCase
- `app/src/main/java/com/swparks/ui/viewmodel/AuthViewModel.kt` - ViewModel авторизации
- `app/src/main/java/com/swparks/domain/usecase/ILoginUseCase.kt` - Интерфейс для LoginUseCase
- `app/src/main/java/com/swparks/domain/usecase/ILogoutUseCase.kt` - Интерфейс для LogoutUseCase
- `app/src/test/java/com/swparks/ui/viewmodel/AuthViewModelTest.kt` - Unit тесты для AuthViewModel
- Обновлены `app/src/main/java/com/swparks/domain/usecase/LoginUseCase.kt`, `LogoutUseCase.kt`, `AppContainer.kt`

**Результат:** Все тесты проходят успешно ✅

---

## Итог

После реализации всех этапов:

✅ Токен авторизации хранится в Preferences DataStore с шифрованием через Tink
✅ CryptoManager использует Android Keystore для безопасного хранения ключей
✅ TokenInterceptor автоматически добавляет токен в заголовок Authorization для всех запросов
✅ LoginUseCase сохраняет токен перед вызовом API
✅ LogoutUseCase очищает токен и сбрасывает флаг авторизации
✅ Архитектура консистентна с iOS-версией (Keychain + шифрование)

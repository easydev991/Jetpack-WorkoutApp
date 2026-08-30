# План безопасной работы с токеном авторизации

## Обзор

Документ описывает реализацию безопасного хранения и использования токена авторизации в Android-приложении Jetpack-WorkoutApp с соблюдением консистентности с iOS-версией.

**Последнее обновление:** 30 сентября 2026 года

**Статус проверки:** Все компоненты проверены и соответствуют текущему состоянию проекта

---

## Статус реализации

**Статус:** ✅ **ЗАВЕРШЕНО**

### Реализованные компоненты

- ✅ **CryptoManagerImpl** - AES-128-GCM шифрование через Tink (шаблон `AES128_GCM`), ключи в Android Keystore, интеграционные тесты
- ✅ **EncryptedStringSerializer** - автоматическое шифрование/дешифрование строк с UTF-8 конвертацией
- ✅ **SecureTokenRepository** - Flow authToken, in-memory кэш для синхронного доступа из Interceptor, методы saveAuthToken/getAuthTokenSync/clearAuthTokenSync/loadTokenToCache, unit-тесты
- ✅ **TokenInterceptor** - добавляет `Authorization: Basic {token}`, проверяет пустоту, интегрирован в OkHttpClient, unit-тесты
- ✅ **AppContainer** - создает и интегрирует TokenInterceptor и SecureTokenRepository
- ✅ **TokenEncoder** - генерирует Base64-токен `login:password` из учётных данных
- ✅ **LoginUseCase** - генерирует токен через TokenEncoder, сохраняет токен, вызывает login, сохраняет userId, unit-тесты
- ✅ **LogoutUseCase** - очищает токен, сбрасывает авторизацию (forceLogout), очищает локальные данные пользователя, unit-тесты
- ✅ **AuthViewModel** - использует LoginUseCase/LogoutUseCase, AuthUiState, UserNotifier, unit-тесты

### Проверка тестов

✅ Все тесты проходят: `SecureTokenRepositoryTest`, `TokenInterceptorTest`, `LoginUseCaseTest`, `LogoutUseCaseTest`, `AuthViewModelTest`, `CryptoManagerIntegrationTest`

**Команда:** `./gradlew :app:testDebugUnitTest` - **BUILD SUCCESSFUL**

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
│       CryptoManagerImpl (Tink)      │
│  - Aead шифрование                   │
│  - AES-128-GCM (AES128_GCM)       │
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

1. **CryptoManagerImpl** (`com.swparks.data.crypto`) - Управление шифрованием
   - Создание ключа в Android Keystore (если не существует)
   - Хранение Tink keyset в SharedPreferences
   - Методы `encrypt(data: ByteArray): ByteArray` и `decrypt(ciphertext: ByteArray): ByteArray`
   - Использует `Aead` с шаблоном `AES128_GCM`

2. **SecureTokenRepository** - API для работы с защищенным токеном
   - Сохранение токена с шифрованием через Preferences DataStore
   - Чтение и расшифрование токена
   - Синхронное очищение для использования в Interceptor

3. **TokenInterceptor** - Добавление токена в заголовки
   - Получает токен из `SecureTokenRepository`
   - Добавляет заголовок `Authorization: Basic {token}` в запросы
   - Не добавляет заголовок если токена нет

4. **AuthInterceptor** - Обработка ошибок 401
   - При 401 (кроме эндпоинтов login, register, reset-password) очищает данные авторизации через `UserPreferencesRepository.clearAllUserData()` — удаляет `currentUserId`, из которого вычисляется `isAuthorized`
   - Защита от повторного логаута через `Mutex`
   - `secureTokenRepository.clearAuthTokenSync()` при 401 не вызывается

---

## План TDD реализации

**Статус:** ✅ Все этапы выполнены

### Выполненные этапы

- ✅ **Подготовка** - добавлена зависимость Tink, подключен Preferences DataStore
- ✅ **Криптография** - CryptoManagerImpl с AES-128-GCM (шаблон Tink `AES128_GCM`) и Android Keystore, интеграционные тесты
- ✅ **SecureTokenRepository** - Flow токена, in-memory кэш, методы для сохранения/чтения/очистки токена, unit-тесты
- ✅ **EncryptedStringSerializer** - автоматическое шифрование/дешифрование строк
- ✅ **TokenInterceptor** - добавляет `Authorization: Basic {token}`, unit-тесты
- ✅ **AppContainer** - интеграция TokenInterceptor и SecureTokenRepository в OkHttp
- ✅ **LoginUseCase** - генерирует токен через TokenEncoder, сохраняет токен, вызывает API, сохраняет userId, unit-тесты
- ✅ **LogoutUseCase** - очищает токен, сбрасывает авторизацию, очищает локальные данные пользователя, unit-тесты
- ✅ **AuthViewModel** - управление состоянием авторизации, AuthUiState, UserNotifier, unit-тесты

---

## Итог

**Реализация полностью завершена и протестирована** ✅

### Функциональность

✅ Безопасное хранение токена в Preferences DataStore с шифрованием через Tink (AES-128-GCM, шаблон `AES128_GCM`)
✅ Ключи шифрования в Android Keystore
✅ Автоматическое добавление токена в заголовок `Authorization: Basic {token}` через TokenInterceptor
✅ LoginUseCase сохраняет токен и userId, LogoutUseCase очищает данные
✅ Консистентность с iOS-версией (Keychain + шифрование)

### Тестирование

✅ Все unit-тесты и интеграционные тесты проходят успешно

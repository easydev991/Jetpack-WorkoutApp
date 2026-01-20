# Сравнение эндпоинтов iOS и Android

## Обзор

Детальное сравнение всех API-эндпоинтов из iOS-приложения (SwiftUI-WorkoutApp) с реализацией в Android-приложении (Jetpack-WorkoutApp).

---

## ✅ Реализованные эндпоинты (61 из 62)

**АВТОРИЗАЦИЯ И ПРОФИЛЬ (6/7):** `login`, `resetPassword`, `editUser`, `changePassword`, `deleteUser`, `getUser`, `getSocialUpdates` ✅

**ДРУЗЬЯ И ЧЕРНЫЙ СПИСОК (10/10):** `getFriendsForUser`, `getFriendRequests`, `acceptFriendRequest`, `declineFriendRequest`, `sendFriendRequest`, `deleteFriend`, `getBlacklist`, `addToBlacklist`, `deleteFromBlacklist`, `findUsers` ✅

**СТРАНЫ И ГОРОДА (1/1):** `getCountries` ✅

**ПЛОЩАДКИ (15/15):** `getAllParks`, `getUpdatedParks`, `getPark`, `createPark`, `editPark`, `deletePark`, `getParksForUser`, `postTrainHere`, `deleteTrainHere`, `addCommentToPark`, `editParkComment`, `deleteParkComment`, `deleteParkPhoto` ✅

**МЕРОПРИЯТИЯ (12/12):** `getFutureEvents`, `getPastEvents`, `getEvent`, `createEvent`, `editEvent`, `deleteEvent`, `postGoToEvent`, `deleteGoToEvent`, `addCommentToEvent`, `editEventComment`, `deleteEventComment`, `deleteEventPhoto` ✅

**СООБЩЕНИЯ (5/5):** `getDialogs`, `getMessages`, `sendMessageTo`, `markAsRead`, `deleteDialog` ✅

**ДНЕВНИКИ (9/9):** `getJournals`, `getJournal`, `editJournalSettings`, `createJournal`, `getJournalEntries`, `saveJournalEntry`, `editJournalEntry`, `deleteJournalEntry`, `deleteJournal` ✅

**КОММЕНТАРИИ К ДНЕВНИКАМ (3/3):** `addCommentToJournalEntry`, `editJournalEntryComment`, `deleteJournalEntryComment` ✅

**✅ НЕТ РАСХОЖДЕНИЙ ПАРАМЕТРОВ:** Android-реализация полностью соответствует iOS-версии. Параметр `password` отсутствует в `editUser` в обеих версиях, так как для смены пароля используется отдельный эндпоинт `changePassword`.

**❌ ОТСУТСТВУЕТ:** `registration` - POST `/registration` с параметрами MainUserForm (name, fullname, email, password, birth_date, gender, country_id?, city_id?)

---

## ❌ Отсутствующие эндпоинты (1 из 62)

### 1. Регистрация пользователя

**iOS Endpoint:**

```swift
/// **POST** ${API}/registration
case registration(form: MainUserForm)
```

**Параметры запроса:**

- `name`: String
- `fullname`: String
- `email`: String
- `password`: String
- `birth_date`: String (ISO 8601)
- `gender`: String ("m", "f", "u")
- `country_id`: Int? (опционально)
- `city_id`: Int? (опционально)

**Android:** ❌ НЕ РЕАЛИЗОВАНО

**План реализации:**

1. Создать модель запроса регистрации:

```kotlin
@Serializable
data class RegistrationRequest(
    val name: String,
    val fullname: String,
    val email: String,
    val password: String,
    val birth_date: String,
    val gender: String,
    @SerialName("country_id")
    val countryId: Int?,
    @SerialName("city_id")
    val cityId: Int?
)
```

2. Добавить метод в `SWApi`:

```kotlin
@POST("registration")
suspend fun registration(@Body request: RegistrationRequest): LoginSuccess
```

3. Добавить метод в `SWRepository`:

```kotlin
suspend fun registration(form: MainUserForm): Result<LoginSuccess>
```

---

## ⚠️ Расхождения в параметрах (0 эндпоинтов)

**✅ НЕТ РАСХОЖДЕНИЙ** - Android-реализация полностью соответствует iOS-версии.

---

## Итоговая статистика

| Категория | Всего в iOS | Реализовано | Отсутствует | Расхождения |
|-----------|-------------|-------------|-------------|------------|
| Авторизация и профиль | 7 | 6 | 1 (registration) | 0 |
| Друзья и черный список | 10 | 10 | 0 | 0 |
| Страны и города | 1 | 1 | 0 | 0 |
| Площадки | 15 | 15 | 0 | 0 |
| Мероприятия | 12 | 12 | 0 | 0 |
| Сообщения | 5 | 5 | 0 | 0 |
| Дневники | 9 | 9 | 0 | 0 |
| Комментарии к дневникам | 3 | 3 | 0 | 0 |
| **ИТОГО** | **62** | **61** | **1** | **0** |

---

## Приоритеты реализации

### Высокий приоритет

1. **Регистрация пользователя** (1 эндпоинт) - критичный функционал для новых пользователей

### Средний приоритет

Нет задач среднего приоритета - все API полностью совместимы с iOS.

### Низкий приоритет

2. **Рефакторинг kotlinx.serialization** - улучшение архитектуры и типизации дат

---

## Дополнительные проверки

### Multipart-запросы

**Проверено:**

- ✅ `editUser` - multipart с изображением
- ✅ `createPark` / `editPark` - multipart с фото
- ✅ `createEvent` / `editEvent` - multipart с фото

**iOS имеет:**

- Формат: `BodyMaker.Parts` с параметрами `parameters: [String: Any]` и `mediaFiles: [MediaFile]?`
- Ключи: `name`, `fullname`, `email`, `gender`, `birth_date`, `country_id`, `city_id`, `image` для editUser
- Ключи: `address`, `latitude`, `longitude`, `city_id`, `type_id`, `class_id` для площадок + photo_0, photo_1...
- Ключи: `title`, `description`, `date`, `area_id` для мероприятий + photo_0, photo_1...

**Android использует:**

- `@Multipart` аннотация Retrofit
- `@Part` для каждого параметра
- `List<MultipartBody.Part>` для фото с именами `photo_0`, `photo_1`...

**✅ СОВПАДАЕТ:** Структура multipart-запросов полностью совпадает.

---

## Заключение

**Статус:** 98.4% реализовано (61 из 62 эндпоинтов)

**Необходимо доделать:**

1. ✅ Регистрация пользователя (1 эндпоинт) - отсутствует полностью

**Рекомендации:**

- Добавить регистрацию для полноценной поддержки новых пользователей

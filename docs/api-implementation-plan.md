# План реализации API в Jetpack-WorkoutApp

## Обзор

Этот документ описывает план по реализации всех запросов к серверу workout.su в Android-приложении, аналогично iOS-версии.

## Текущее состояние

- Базовая структура проекта настроена
- Реализована только одна тестовая функция API: `getPastEvents()`

### Статус существующих моделей

**Полностью реализованные модели (без доработок):**

- ✅ `User`, `Park`, `Event`, `Photo`, `Country`, `City`

**Модели, требующие доработки:**

- ⚠️ `LoginSuccess` → изменить тип `userID` с `Int` на `Long`
- ⚠️ `Comment` → сделать поля `body`, `date`, `user` опциональными, переименовать `commentID` в `id`
- ⚠️ `FriendAction` → создать отдельный enum `ApiFriendAction` для API (текущий сохранить для UI с ресурсами)
- ⚠️ `BlacklistAction` → создать отдельный enum `ApiBlacklistOption` для API (текущий сохранить для UI с ресурсами)

**Модели, которых нет в проекте:**

- ❌ `JournalResponse`, `JournalEntryResponse`
- ❌ `DialogResponse`, `MessageResponse`
- ❌ `MainUserForm`, `ParkForm`, `EventForm`
- ❌ `ChangePasswordRequest`, `EditJournalSettingsRequest`, `MarkAsReadRequest`
- ❌ `EventType`, `JournalAccess`, `TextEntryOption`
- ❌ `ApiFriendAction`, `ApiBlacklistOption`

## Структура API протоколов в iOS

В iOS-проекте используются следующие протоколы клиентов:

1. **AuthClient** - авторизация и восстановление пароля
2. **CommentsClient** - работа с комментариями и записями
3. **CountriesClient** - справочник стран/городов
4. **EventsClient** - мероприятия
5. **FriendsClient** - друзья и социальные функции
6. **JournalsClient** - дневники
7. **MessagesClient** - сообщения и диалоги
8. **ParksClient** - площадки
9. **ParksUpdaterClient** - обновленные площадки
10. **PhotosClient** - фотографии
11. **ProfileClient** - профиль пользователя

---

## Этап 1: Модели данных ответов сервера

### 1.1. Авторизация и профиль

#### LoginResponse

```kotlin
@Serializable
data class LoginResponse(
    @SerialName("user_id")
    val userId: Long
)
```

**Примечание:** Модель существует как `LoginSuccess` (тип `userID` нужно изменить с `Int` на `Long`).

#### MainUserForm

```kotlin
@Serializable
data class MainUserForm(
    val name: String,
    val fullname: String,
    val email: String,
    val password: String,
    @SerialName("birth_date")
    val birthDate: String,
    @SerialName("gender")
    val genderCode: Int,
    @SerialName("country_id")
    val countryId: Int? = null,
    @SerialName("city_id")
    val cityId: Int? = null
)
```

### 1.2. Комментарии

#### Comment

```kotlin
@Serializable
data class Comment(
    @SerialName("comment_id")
    val id: Long,
    val body: String?,
    val date: String?,
    val user: User?
)
```

**Примечание:** Модель существует, но поля `body`, `date`, `user` обязательны, а поле называется `commentID`.

### 1.3. Дневники

#### JournalResponse

```kotlin
@Serializable
data class JournalResponse(
    @SerialName("journal_id")
    val id: Long,
    val title: String?,
    @SerialName("last_message_image")
    val lastMessageImage: String?,
    @SerialName("create_date")
    val createDate: String?,
    @SerialName("modify_date")
    val modifyDate: String?,
    @SerialName("last_message_date")
    val lastMessageDate: String?,
    @SerialName("last_message_text")
    val lastMessageText: String?,
    val count: Int?,
    @SerialName("user_id")
    val ownerId: Int?,
    @SerialName("view_access")
    val viewAccess: Int?,
    @SerialName("comment_access")
    val commentAccess: Int?
)
```

#### JournalEntryResponse

```kotlin
@Serializable
data class JournalEntryResponse(
    val id: Long,
    @SerialName("journal_id")
    val journalId: Int?,
    @SerialName("user_id")
    val authorId: Int?,
    val name: String?,
    val message: String?,
    @SerialName("create_date")
    val createDate: String?,
    @SerialName("modify_date")
    val modifyDate: String?,
    val image: String?
)
```

### 1.4. Сообщения

#### DialogResponse

```kotlin
@Serializable
data class DialogResponse(
    @SerialName("dialog_id")
    val id: Long,
    @SerialName("user_id")
    val anotherUserId: Int?,
    val name: String?,
    val image: String?,
    @SerialName("last_message_text")
    val lastMessageText: String?,
    @SerialName("last_message_date")
    val lastMessageDate: String?,
    val count: Int?
)
```

#### MessageResponse

```kotlin
@Serializable
data class MessageResponse(
    val id: Long,
    @SerialName("user_id")
    val userId: Int?,
    val message: String?,
    val name: String?,
    val created: String?
)
```

### 1.5. Формы для отправки данных

#### ParkForm

```kotlin
@Serializable
data class ParkForm(
    val address: String,
    val latitude: String,
    val longitude: String,
    @SerialName("city_id")
    val cityId: Int?,
    @SerialName("type_id")
    val typeId: Int,
    @SerialName("class_id")
    val sizeId: Int
)
```

#### EventForm

```kotlin
@Serializable
data class EventForm(
    val title: String,
    val description: String,
    val date: String,
    @SerialName("area_id")
    val parkId: Long
)
```

### 1.6. Перечисления

#### TextEntryOption (тип записи: площадка/мероприятие/дневник)

```kotlin
sealed class TextEntryOption {
    data class Park(val id: Int) : TextEntryOption()
    data class Event(val id: Int) : TextEntryOption()
    data class Journal(val ownerId: Int, val journalId: Int) : TextEntryOption()
}
```

#### FriendAction (API-модель)

```kotlin
enum class FriendAction {
    ADD,
    REMOVE
}
```

**Примечание:** Создать отдельный enum `ApiFriendAction` для API, сохранить текущий `FriendAction` с ресурсными строками для UI.

#### BlacklistOption (API-модель)

```kotlin
enum class BlacklistOption {
    ADD,
    REMOVE
}
```

**Примечание:** Создать отдельный enum `ApiBlacklistOption` для API, сохранить текущий `BlacklistAction` с ресурсными строками для UI.

#### JournalAccess

```kotlin
enum class JournalAccess(val rawValue: Int) {
    ALL(0),
    FRIENDS(1),
    NOBODY(2);

    companion object {
        fun from(rawValue: Int?): JournalAccess {
            return when (rawValue) {
                0 -> ALL
                1 -> FRIENDS
                2 -> NOBODY
                else -> ALL
            }
        }
    }
}
```

#### EventType

```kotlin
enum class EventType {
    FUTURE,
    PAST
}
```

---

## Этап 2: SWApi - API интерфейс

### 2.1. Авторизация и профиль

```kotlin
interface SWApi {
    // Авторизация
    @POST("auth/login")
    suspend fun login(@Body token: String?): LoginResponse

    // Сброс пароля
    @POST("auth/reset")
    suspend fun resetPassword(@Body login: String): LoginResponse

    // Смена пароля
    @POST("auth/changepass")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    // Получить профиль пользователя
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: Long): User

    // Изменить данные пользователя
    @POST("users/{userId}")
    @Multipart
    suspend fun editUser(
        @Path("userId") userId: Long,
        @Part("name") name: String,
        @Part("fullname") fullName: String,
        @Part("email") email: String,
        @Part("birth_date") birthDate: String,
        @Part("gender") gender: String,
        @Part("country_id") countryId: Int?,
        @Part("city_id") cityId: Int?,
        @Part image: MultipartBody.Part?
    ): User

    // Удалить профиль текущего пользователя
    @DELETE("users/current")
    suspend fun deleteUser(): Response<Unit>
}
```

### 2.2. Друзья и черный список

```kotlin
interface SWApi {
    // Получить список друзей пользователя
    @GET("users/{userId}/friends")
    suspend fun getFriendsForUser(@Path("userId") userId: Long): List<User>

    // Получить список заявок на добавление в друзья
    @GET("friends/requests")
    suspend fun getFriendRequests(): List<User>

    // Принять заявку на добавление в друзья
    @POST("friends/{userId}/accept")
    suspend fun acceptFriendRequest(@Path("userId") userId: Long): Response<Unit>

    // Отклонить заявку на добавление в друзья
    @DELETE("friends/{userId}/accept")
    suspend fun declineFriendRequest(@Path("userId") userId: Long): Response<Unit>

    // Отправить запрос на добавление в друзья
    @POST("friends/{userId}")
    suspend fun sendFriendRequest(@Path("userId") userId: Long): Response<Unit>

    // Удалить пользователя из списка друзей
    @DELETE("friends/{userId}")
    suspend fun deleteFriend(@Path("userId") friendId: Long): Response<Unit>

    // Получить черный список пользователей
    @GET("blacklist")
    suspend fun getBlacklist(): List<User>

    // Добавить пользователя в черный список
    @POST("blacklist/{userId}")
    suspend fun addToBlacklist(@Path("userId") userId: Long): Response<Unit>

    // Удалить пользователя из черного списка
    @DELETE("blacklist/{userId}")
    suspend fun deleteFromBlacklist(@Path("userId") userId: Long): Response<Unit>

    // Найти пользователей по логину
    @GET("users/search")
    suspend fun findUsers(@Query("name") name: String): List<User>
}
```

### 2.3. Страны и города

```kotlin
interface SWApi {
    // Получить список стран/городов
    @GET("countries")
    suspend fun getCountries(): List<Country>
}
```

### 2.4. Площадки

```kotlin
interface SWApi {
    // Получить список всех площадок (краткий)
    @GET("areas")
    suspend fun getAllParks(@Query("fields") fields: String = "short"): List<Park>

    // Получить список площадок, обновленных после указанной даты
    @GET("areas/last/{date}")
    suspend fun getUpdatedParks(@Path("date") date: String): List<Park>

    // Получить выбранную площадку
    @GET("areas/{parkId}")
    suspend fun getPark(@Path("parkId") parkId: Long): Park

    // Добавить новую площадку
    @POST("areas")
    @Multipart
    suspend fun createPark(
        @Part("address") address: String,
        @Part("latitude") latitude: String,
        @Part("longitude") longitude: String,
        @Part("city_id") cityId: Int?,
        @Part("type_id") typeId: Int,
        @Part("class_id") sizeId: Int,
        @Part photos: List<MultipartBody.Part>?
    ): Park

    // Изменить выбранную площадку
    @POST("areas/{parkId}")
    @Multipart
    suspend fun editPark(
        @Path("parkId") parkId: Long,
        @Part("address") address: String,
        @Part("latitude") latitude: String,
        @Part("longitude") longitude: String,
        @Part("city_id") cityId: Int?,
        @Part("type_id") typeId: Int,
        @Part("class_id") sizeId: Int,
        @Part photos: List<MultipartBody.Part>?
    ): Park

    // Удалить площадку
    @DELETE("areas/{parkId}")
    suspend fun deletePark(@Path("parkId") parkId: Long): Response<Unit>

    // Получить список площадок, где тренируется пользователь
    @GET("users/{userId}/areas")
    suspend fun getParksForUser(@Path("userId") userId: Long): List<Park>

    // Сообщить, что пользователь тренируется на площадке
    @POST("areas/{parkId}/train")
    suspend fun postTrainHere(@Path("parkId") parkId: Long): Response<Unit>

    // Сообщить, что пользователь не тренируется на площадке
    @DELETE("areas/{parkId}/train")
    suspend fun deleteTrainHere(@Path("parkId") parkId: Long): Response<Unit>

    // Добавить комментарий для площадки
    @POST("areas/{parkId}/comments")
    suspend fun addCommentToPark(
        @Path("parkId") parkId: Long,
        @Body comment: String
    ): Response<Unit>

    // Изменить свой комментарий для площадки
    @POST("areas/{parkId}/comments/{commentId}")
    suspend fun editParkComment(
        @Path("parkId") parkId: Long,
        @Path("commentId") commentId: Long,
        @Body comment: String
    ): Response<Unit>

    // Удалить свой комментарий для площадки
    @DELETE("areas/{parkId}/comments/{commentId}")
    suspend fun deleteParkComment(
        @Path("parkId") parkId: Long,
        @Path("commentId") commentId: Long
    ): Response<Unit>

    // Удалить фото площадки
    @DELETE("areas/{parkId}/photos/{photoId}")
    suspend fun deleteParkPhoto(
        @Path("parkId") parkId: Long,
        @Path("photoId") photoId: Long
    ): Response<Unit>
}
```

### 2.5. Мероприятия

```kotlin
interface SWApi {
    // Получить список предстоящих мероприятий
    @GET("trainings/current")
    suspend fun getFutureEvents(): List<Event>

    // Получить краткий список прошедших мероприятий
    @GET("trainings/last")
    suspend fun getPastEvents(): List<Event>

    // Получить всю информацию о мероприятии
    @GET("trainings/{eventId}")
    suspend fun getEvent(@Path("eventId") eventId: Long): Event

    // Создать новое мероприятие
    @POST("trainings")
    @Multipart
    suspend fun createEvent(
        @Part("title") title: String,
        @Part("description") description: String,
        @Part("date") date: String,
        @Part("area_id") parkId: Long,
        @Part photos: List<MultipartBody.Part>?
    ): Event

    // Изменить существующее мероприятие
    @POST("trainings/{eventId}")
    @Multipart
    suspend fun editEvent(
        @Path("eventId") eventId: Long,
        @Part("title") title: String,
        @Part("description") description: String,
        @Part("date") date: String,
        @Part("area_id") parkId: Long,
        @Part photos: List<MultipartBody.Part>?
    ): Event

    // Сообщить, что пользователь пойдет на мероприятие
    @POST("trainings/{eventId}/go")
    suspend fun postGoToEvent(@Path("eventId") eventId: Long): Response<Unit>

    // Сообщить, что пользователь не пойдет на мероприятие
    @DELETE("trainings/{eventId}/go")
    suspend fun deleteGoToEvent(@Path("eventId") eventId: Long): Response<Unit>

    // Добавить комментарий для мероприятия
    @POST("trainings/{eventId}/comments")
    suspend fun addCommentToEvent(
        @Path("eventId") eventId: Long,
        @Body comment: String
    ): Response<Unit>

    // Удалить свой комментарий для мероприятия
    @DELETE("trainings/{eventId}/comments/{commentId}")
    suspend fun deleteEventComment(
        @Path("eventId") eventId: Long,
        @Path("commentId") commentId: Long
    ): Response<Unit>

    // Изменить свой комментарий для мероприятия
    @POST("trainings/{eventId}/comments/{commentId}")
    suspend fun editEventComment(
        @Path("eventId") eventId: Long,
        @Path("commentId") commentId: Long,
        @Body comment: String
    ): Response<Unit>

    // Удалить мероприятие
    @DELETE("trainings/{eventId}")
    suspend fun deleteEvent(@Path("eventId") eventId: Long): Response<Unit>

    // Удалить фото мероприятия
    @DELETE("trainings/{eventId}/photos/{photoId}")
    suspend fun deleteEventPhoto(
        @Path("eventId") eventId: Long,
        @Path("photoId") photoId: Long
    ): Response<Unit>
}
```

### 2.6. Сообщения

```kotlin
interface SWApi {
    // Получить список диалогов
    @GET("dialogs")
    suspend fun getDialogs(): List<DialogResponse>

    // Получить сообщения в диалоге
    @GET("dialogs/{dialogId}/messages")
    suspend fun getMessages(@Path("dialogId") dialogId: Long): List<MessageResponse>

    // Отправить сообщение пользователю
    @POST("messages/{userId}")
    suspend fun sendMessageTo(
        @Path("userId") userId: Long,
        @Body message: String
    ): Response<Unit>

    // Отметить сообщения как прочитанные
    @POST("messages/mark_as_read")
    suspend fun markAsRead(@Body userId: Long): Response<Unit>

    // Удалить выбранный диалог
    @DELETE("dialogs/{dialogId}")
    suspend fun deleteDialog(@Path("dialogId") dialogId: Long): Response<Unit>
}
```

### 2.7. Дневники

```kotlin
interface SWApi {
    // Получить список дневников пользователя
    @GET("users/{userId}/journals")
    suspend fun getJournals(@Path("userId") userId: Long): List<JournalResponse>

    // Получить дневник пользователя
    @GET("users/{userId}/journals/{journalId}")
    suspend fun getJournal(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long
    ): JournalResponse

    // Изменить настройки дневника
    @PUT("users/{userId}/journals/{journalId}")
    suspend fun editJournalSettings(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Body request: EditJournalSettingsRequest
    ): Response<Unit>

    // Создать новый дневник
    @POST("users/{userId}/journals")
    suspend fun createJournal(
        @Path("userId") userId: Long,
        @Body title: String
    ): Response<Unit>

    // Получить записи из дневника пользователя
    @GET("users/{userId}/journals/{journalId}/messages")
    suspend fun getJournalEntries(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long
    ): List<JournalEntryResponse>

    // Сохранить новую запись в дневнике пользователя
    @POST("users/{userId}/journals/{journalId}/messages")
    suspend fun saveJournalEntry(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Body message: String
    ): Response<Unit>

    // Изменить запись в дневнике пользователя
    @PUT("users/{userId}/journals/{journalId}/messages/{entryId}")
    suspend fun editJournalEntry(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Path("entryId") entryId: Long,
        @Body newEntryText: String
    ): Response<Unit>

    // Удалить запись в дневнике пользователя
    @DELETE("users/{userId}/journals/{journalId}/messages/{entryId}")
    suspend fun deleteJournalEntry(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Path("entryId") entryId: Long
    ): Response<Unit>

    // Удалить дневник пользователя
    @DELETE("users/{userId}/journals/{journalId}")
    suspend fun deleteJournal(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long
    ): Response<Unit>
}
```

---

## Этап 3: SWRepository - обновление репозитория

### 3.1. Авторизация

```kotlin
suspend fun login(token: String?): Result<LoginSuccess>
suspend fun resetPassword(login: String): Result<Unit>
suspend fun changePassword(current: String, new: String): Result<Unit>
```

### 3.2. Профиль

```kotlin
suspend fun getUser(userId: Long): Result<User>
suspend fun editUser(userId: Long, form: MainUserForm, image: ByteArray?): Result<User>
suspend fun deleteUser(): Result<Unit>
suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates>
suspend fun findUsers(name: String): Result<List<User>>
```

### 3.3. Друзья

```kotlin
suspend fun getFriendsForUser(userId: Long): Result<List<User>>
suspend fun getFriendRequests(): Result<List<User>>
suspend fun respondToFriendRequest(userId: Long, accept: Boolean): Result<Unit>
suspend fun friendAction(userId: Long, action: ApiFriendAction): Result<Unit>
suspend fun blacklistAction(user: User, option: ApiBlacklistOption): Result<Unit>
suspend fun getBlacklist(): Result<List<User>>
```

**Примечание:** Методы `friendAction` и `blacklistAction` используют API-модели (`ApiFriendAction`, `ApiBlacklistOption`) вместо UI-моделей. Маппинг через extension-функции.

### 3.4. Площадки

```kotlin
suspend fun getAllParks(): Result<List<Park>>
suspend fun getPark(id: Long): Result<Park>
suspend fun savePark(id: Long?, form: ParkForm, photos: List<ByteArray>?): Result<Park>
suspend fun deletePark(parkId: Long): Result<Unit>
suspend fun getParksForUser(userId: Long): Result<List<Park>>
suspend fun changeTrainHereStatus(trainHere: Boolean, parkId: Long): Result<Unit>
suspend fun getUpdatedParks(date: String): Result<List<Park>>
```

### 3.5. Мероприятия

```kotlin
suspend fun getEvents(type: EventType): Result<List<Event>>
suspend fun getEvent(id: Long): Result<Event>
suspend fun saveEvent(id: Long?, form: EventForm, photos: List<ByteArray>?): Result<Event>
suspend fun changeIsGoingToEvent(go: Boolean, eventId: Long): Result<Unit>
suspend fun deleteEvent(eventId: Long): Result<Unit>
```

### 3.6. Сообщения

```kotlin
suspend fun getDialogs(): Result<List<DialogResponse>>
suspend fun getMessages(dialogId: Long): Result<List<MessageResponse>>
suspend fun sendMessage(message: String, userId: Long): Result<Unit>
suspend fun markAsRead(userId: Long): Result<Unit>
suspend fun deleteDialog(dialogId: Long): Result<Unit>
```

### 3.7. Дневники

```kotlin
suspend fun getJournals(userId: Long): Result<List<JournalResponse>>
suspend fun getJournal(userId: Long, journalId: Long): Result<JournalResponse>
suspend fun editJournalSettings(journalId: Long, title: String, userId: Long?, viewAccess: JournalAccess, commentAccess: JournalAccess): Result<Unit>
suspend fun createJournal(title: String, userId: Long?): Result<Unit>
suspend fun getJournalEntries(userId: Long, journalId: Long): Result<List<JournalEntryResponse>>
suspend fun deleteJournal(journalId: Long, userId: Long?): Result<Unit>
```

### 3.8. Комментарии

```kotlin
suspend fun addComment(option: TextEntryOption, comment: String): Result<Unit>
suspend fun editComment(option: TextEntryOption, commentId: Long, newComment: String): Result<Unit>
suspend fun deleteComment(option: TextEntryOption, commentId: Long): Result<Unit>
```

---

## Этап 4: Вспомогательные модели и утилиты

### 4.1. API-модели для перечислений

**Важно:** Создать отдельные API-модели `ApiFriendAction` и `ApiBlacklistOption`. Текущие `FriendAction` и `BlacklistAction` с ресурсными строками сохранить для UI.

#### ApiFriendAction

```kotlin
/**
 * API-модель для действий с друзьями (без ресурсов)
 */
enum class ApiFriendAction {
    ADD,    // Добавить в друзья
    REMOVE  // Удалить из друзей
}

// Extension-функция для маппинга
fun FriendAction.toApiAction(): ApiFriendAction = when (this) {
    FriendAction.SEND_FRIEND_REQUEST -> ApiFriendAction.ADD
    FriendAction.REMOVE_FRIEND -> ApiFriendAction.REMOVE
}
```

#### ApiBlacklistOption

```kotlin
/**
 * API-модель для действий с черным списком (без ресурсов)
 */
enum class ApiBlacklistOption {
    ADD,    // Добавить в черный список
    REMOVE  // Удалить из черного списка
}

// Extension-функция для маппинга
fun BlacklistAction.toApiOption(): ApiBlacklistOption = when (this) {
    BlacklistAction.BLOCK -> ApiBlacklistOption.ADD
    BlacklistAction.UNBLOCK -> ApiBlacklistOption.REMOVE
}
```

### 4.2. Models для запросов

```kotlin
@Serializable
data class ChangePasswordRequest(
    val password: String,
    @SerialName("new_password")
    val newPassword: String
)

@Serializable
data class EditJournalSettingsRequest(
    val title: String,
    @SerialName("view_access")
    val viewAccess: String,
    @SerialName("comment_access")
    val commentAccess: String
)

@Serializable
data class MarkAsReadRequest(
    @SerialName("from_user_id")
    val fromUserId: Long
)
```

### 4.3. Утилиты для multipart

```kotlin
object NetworkUtils {
    fun createImagePart(data: ByteArray, name: String): MultipartBody.Part {
        val requestFile = data.toRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, "$name.jpg", requestFile)
    }
}
```

---

## Этап 5: Тестирование

### 5.1. Unit тесты для моделей

- `LoginResponseTest`
- `DialogResponseTest`
- `MessageResponseTest`
- `JournalResponseTest`
- `JournalEntryResponseTest`
- `CommentTest`
- `MainUserFormTest`
- `ParkFormTest`
- `EventFormTest`

### 5.2. Моки для API

Создать `MockSWApi` для тестирования с фиктивными данными.

### 5.3. Unit тесты для репозитория

- `SWRepositoryAuthTest` - тесты авторизации
- `SWRepositoryProfileTest` - тесты профиля
- `SWRepositoryFriendsTest` - тесты друзей
- `SWRepositoryParksTest` - тесты площадок
- `SWRepositoryEventsTest` - тесты мероприятий
- `SWRepositoryMessagesTest` - тесты сообщений
- `SWRepositoryJournalsTest` - тесты дневников
- `SWRepositoryCommentsTest` - тесты комментариев

---

## Этап 6: Интеграция с DI

### 6.1. Обновить AppContainer

Добавить фабричные методы для создания Use Cases с новым API:

```kotlin
fun provideAuthApi(): SWApi
fun provideProfileApi(): SWApi
fun provideFriendsApi(): SWApi
fun provideParksApi(): SWApi
fun provideEventsApi(): SWApi
fun provideMessagesApi(): SWApi
fun provideJournalsApi(): SWApi
```

---

## Приоритет реализации

### Высокий приоритет (для базовой функциональности)

1. **Этап 1**: Реализовать основные модели данных
   - LoginResponse
   - DialogResponse
   - MessageResponse
   - JournalResponse
   - JournalEntryResponse
   - MainUserForm, ParkForm, EventForm
   - Comment

2. **Этап 2**: Реализовать базовые API endpoints
   - Авторизация (login, resetPassword, changePassword)
   - Профиль (getUser, editUser)
   - Площадки (getPark, getParksForUser, createPark, editPark)
   - Мероприятия (getEvents, getEvent, createEvent, editEvent)

3. **Этап 3**: Обновить SWRepository
   - Методы авторизации
   - Методы профиля
   - Методы площадок
   - Методы мероприятий

### Средний приоритет (для расширенной функциональности)

4. **Этап 2 продолжение**: API endpoints для друзей
5. **Этап 2 продолжение**: API endpoints для сообщений
6. **Этап 2 продолжение**: API endpoints для дневников
7. **Этап 3 продолжение**: Обновить SWRepository для друзей, сообщений, дневников

### Низкий приоритет (дополнительный функционал)

8. API endpoints для комментариев
9. API endpoints для фото
10. Полные тесты для всех компонентов

---

## Примечания

1. **Безопасное разворачивание опционалов**: Всегда использовать `?.let`, `?:`, `checkNotNull` вместо `!!`
2. **Обработка ошибок**: Использовать `Result<T>` для API ответов
3. **Логирование**: Логи на русском языке
4. **Локализация**: Поддержка русского (ru) и английского (en) языков
5. **Тестирование**: Все сетевые функции тестировать только на моках, без реальных запросов
6. **Стиль кода**: Следовать правилам из `.cursor/rules/code-style.mdc`
7. **Архитектура**: MVVM + Clean Architecture, ручной DI (без Hilt)

---

## Результаты выполнения

После завершения всех этапов Android-приложение будет иметь:

1. Полный набор API endpoints для взаимодействия с сервером workout.su
2. Все модели данных ответов сервера, идентичные iOS-версии
3. Репозиторий с методами для всех бизнес-операций
4. Поддержку авторизации, профилей, площадок, мероприятий, друзей, сообщений, дневников
5. Unit тесты для моделей и репозитория
6. Интеграцию с DI через AppContainer

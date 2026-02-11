package com.swparks.network

import com.swparks.data.model.Country
import com.swparks.data.model.DialogResponse
import com.swparks.data.model.Event
import com.swparks.data.model.JournalEntryResponse
import com.swparks.data.model.JournalResponse
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.MessageResponse
import com.swparks.data.model.Park
import com.swparks.data.model.User
import com.swparks.ui.model.RegistrationRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("TooManyFunctions")
interface SWApi {
    // ==================== АВТОРИЗАЦИЯ И ПРОФИЛЬ ====================

    // Регистрация
    @POST("registration")
    suspend fun register(@Body request: RegistrationRequest): LoginSuccess

    // Авторизация
    @POST("auth/login")
    suspend fun login(): LoginSuccess

    // Сброс пароля
    @FormUrlEncoded
    @POST("auth/reset")
    suspend fun resetPassword(@Field("username_or_email") usernameOrEmail: String): LoginSuccess

    // Смена пароля
    @FormUrlEncoded
    @POST("auth/changepass")
    suspend fun changePassword(
        @Field("password") password: String,
        @Field("new_password") newPassword: String
    ): Response<Unit>

    // Получить профиль пользователя
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: Long): User

    // Изменить данные пользователя
    @POST("users/{userId}")
    @Multipart
    @Suppress("LongParameterList")
    suspend fun editUser(
        @Path("userId") userId: Long,
        @Part("name") name: RequestBody,
        @Part("fullname") fullName: RequestBody,
        @Part("email") email: RequestBody,
        @Part("birth_date") birthDate: RequestBody?,
        @Part("gender") gender: RequestBody?,
        @Part("country_id") countryId: RequestBody?,
        @Part("city_id") cityId: RequestBody?,
        @Part image: MultipartBody.Part?
    ): User

    // Удалить профиль текущего пользователя
    @DELETE("users/current")
    suspend fun deleteUser(): Response<Unit>

    // ==================== ДРУЗЬЯ И ЧЕРНЫЙ СПИСОК ====================

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

    // ==================== СТРАНЫ И ГОРОДА ====================

    // Получить список стран/городов
    @GET("countries")
    suspend fun getCountries(): List<Country>

    // ==================== ПЛОЩАДКИ ====================

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
    @Suppress("LongParameterList")
    suspend fun createPark(
        @Part("address") address: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("city_id") cityId: RequestBody?,
        @Part("type_id") typeId: RequestBody,
        @Part("class_id") sizeId: RequestBody,
        @Part photos: List<MultipartBody.Part>?
    ): Park

    // Изменить выбранную площадку
    @POST("areas/{parkId}")
    @Multipart
    @Suppress("LongParameterList")
    suspend fun editPark(
        @Path("parkId") parkId: Long,
        @Part("address") address: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("city_id") cityId: RequestBody?,
        @Part("type_id") typeId: RequestBody,
        @Part("class_id") sizeId: RequestBody,
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

    // ==================== МЕРОПРИЯТИЯ ====================

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
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("date") date: RequestBody,
        @Part("area_id") parkId: RequestBody,
        @Part photos: List<MultipartBody.Part>?
    ): Event

    // Изменить существующее мероприятие
    @POST("trainings/{eventId}")
    @Multipart
    @Suppress("LongParameterList")
    suspend fun editEvent(
        @Path("eventId") eventId: Long,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("date") date: RequestBody,
        @Part("area_id") parkId: RequestBody,
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

    // ==================== СООБЩЕНИЯ ====================

    // Получить список диалогов
    @GET("dialogs")
    suspend fun getDialogs(): List<DialogResponse>

    // Получить сообщения в диалоге
    @GET("dialogs/{dialogId}/messages")
    suspend fun getMessages(@Path("dialogId") dialogId: Long): List<MessageResponse>

    // Отправить сообщение пользователю
    @FormUrlEncoded
    @POST("messages/{userId}")
    suspend fun sendMessageTo(
        @Path("userId") userId: Long,
        @Field("message") message: String
    ): Response<Unit>

    // Отметить сообщения как прочитанные
    @FormUrlEncoded
    @POST("messages/mark_as_read")
    suspend fun markAsRead(@Field("from_user_id") fromUserId: Long): Response<Unit>

    // Удалить выбранный диалог
    @DELETE("dialogs/{dialogId}")
    suspend fun deleteDialog(@Path("dialogId") dialogId: Long): Response<Unit>

    // ==================== ДНЕВНИКИ ====================

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
    @FormUrlEncoded
    @PUT("users/{userId}/journals/{journalId}")
    suspend fun editJournalSettings(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Field("title") title: String,
        @Field("view_access") viewAccess: String,
        @Field("comment_access") commentAccess: String
    ): Response<Unit>

    // Создать новый дневник
    @FormUrlEncoded
    @POST("users/{userId}/journals")
    suspend fun createJournal(
        @Path("userId") userId: Long,
        @Field("title") title: String
    ): Response<Unit>

    // Получить записи из дневника пользователя
    @GET("users/{userId}/journals/{journalId}/messages")
    suspend fun getJournalEntries(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long
    ): List<JournalEntryResponse>

    // Сохранить новую запись в дневнике пользователя
    @FormUrlEncoded
    @POST("users/{userId}/journals/{journalId}/messages")
    suspend fun saveJournalEntry(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Field("message") message: String
    ): Response<Unit>

    // Изменить запись в дневнике пользователя
    @FormUrlEncoded
    @PUT("users/{userId}/journals/{journalId}/messages/{entryId}")
    suspend fun editJournalEntry(
        @Path("userId") userId: Long,
        @Path("journalId") journalId: Long,
        @Path("entryId") entryId: Long,
        @Field("message") newEntryText: String
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
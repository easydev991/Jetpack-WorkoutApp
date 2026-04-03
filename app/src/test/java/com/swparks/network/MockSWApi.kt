package com.swparks.network

import com.swparks.data.model.Comment
import com.swparks.data.model.Country
import com.swparks.data.model.DialogResponse
import com.swparks.data.model.Event
import com.swparks.data.model.JournalEntryResponse
import com.swparks.data.model.JournalResponse
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.MessageResponse
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

/**
 * Mock реализация SWApi для тестирования с фиктивными данными.
 * Возвращает предопределенные данные для всех методов API.
 */
class MockSWApi : SWApi {
    // ==================== Вспомогательные методы для создания фиктивных данных ====================

    /**
     * Создать фиктивный LoginSuccess
     */
    fun createMockLoginSuccess(userId: Long = 123L) = LoginSuccess(userId = userId)

    /**
     * Создать фиктивного пользователя
     */
    fun createMockUser(
        id: Long = 1L,
        name: String = "testuser",
        fullName: String? = "Test User",
        email: String? = "test@example.com"
    ) = User(
        id = id,
        name = name,
        image = "https://example.com/user.jpg",
        cityID = 1,
        countryID = 1,
        birthDate = "1990-01-01",
        email = email,
        fullName = fullName,
        genderCode = 0,
        friendRequestCount = "1",
        friendsCount = 5,
        parksCount = "2",
        addedParks = emptyList(),
        journalCount = 3
    )

    /**
     * Создать фиктивную площадку
     */
    fun createMockPark(
        id: Long = 1L,
        name: String = "Test Park",
        address: String = "Test Address"
    ) = Park(
        id = id,
        name = name,
        sizeID = 1,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = address,
        cityID = 1,
        countryID = 1,
        commentsCount = 5,
        preview = "https://example.com/park.jpg",
        trainingUsersCount = 2,
        createDate = "2024-01-01",
        modifyDate = "2024-01-15",
        author = createMockUser(),
        photos = listOf(createMockPhoto()),
        comments = listOf(createMockComment()),
        trainHere = false,
        equipmentIDS = emptyList(),
        mine = false,
        canEdit = false,
        trainingUsers = emptyList()
    )

    /**
     * Создать фиктивное мероприятие
     */
    fun createMockEvent(
        id: Long = 1L,
        title: String = "Test Event",
        beginDate: String = "2024-02-01"
    ) = Event(
        id = id,
        title = title,
        description = "Test Description",
        beginDate = beginDate,
        countryID = 1,
        cityID = 1,
        preview = "https://example.com/event.jpg",
        latitude = "55.7558",
        longitude = "37.6173",
        isCurrent = false,
        photos = listOf(createMockPhoto()),
        author = createMockUser(),
        name = title
    )

    /**
     * Создать фиктивный диалог
     */
    fun createMockDialog(
        id: Long = 1L,
        userId: Int = 2,
        name: String = "Test User"
    ) = DialogResponse(
        id = id,
        anotherUserId = userId,
        name = name,
        image = "https://example.com/user.jpg",
        lastMessageText = "Hello!",
        lastMessageDate = "2024-01-15",
        count = 1
    )

    /**
     * Создать фиктивное сообщение
     */
    fun createMockMessage(
        id: Long = 1L,
        userId: Int = 2,
        message: String = "Test message",
        created: String = "2024-01-15"
    ) = MessageResponse(
        id = id,
        userId = userId,
        message = message,
        name = "Test User",
        created = created
    )

    /**
     * Создать фиктивный дневник
     */
    fun createMockJournal(
        id: Long = 1L,
        title: String = "Test Journal"
    ) = JournalResponse(
        id = id,
        title = title,
        lastMessageImage = "https://example.com/journal.jpg",
        createDate = "2024-01-01",
        modifyDate = "2024-01-15",
        lastMessageDate = "2024-01-15",
        lastMessageText = "Last entry",
        count = 5,
        ownerId = 1,
        viewAccess = 0,
        commentAccess = 0
    )

    /**
     * Создать фиктивную запись в дневнике
     */
    fun createMockJournalEntry(
        id: Long = 1L,
        message: String = "Test entry"
    ) = JournalEntryResponse(
        id = id,
        journalId = 1,
        authorId = 1,
        name = "Test User",
        message = message,
        createDate = "2024-01-15",
        modifyDate = "2024-01-15",
        image = "https://example.com/entry.jpg"
    )

    /**
     * Создать фиктивный комментарий
     */
    fun createMockComment(
        id: Long = 1L,
        body: String = "Test comment"
    ) = Comment(
        id = id,
        body = body,
        date = "2024-01-15",
        user = createMockUser()
    )

    /**
     * Создать фиктивное фото
     */
    fun createMockPhoto(
        id: Long = 1L,
        photo: String = "https://example.com/photo.jpg"
    ) = Photo(
        id = id,
        photo = photo
    )

    /**
     * Создать фиктивную страну
     */
    fun createMockCountry(
        id: String = "1",
        name: String = "Russia"
    ) = Country(
        id = id,
        name = name,
        cities = listOf()
    )

    // ==================== АВТОРИЗАЦИЯ И ПРОФИЛЬ ====================

    override suspend fun login(): LoginSuccess = createMockLoginSuccess()

    override suspend fun register(
        name: String,
        fullName: String,
        email: String,
        password: String,
        birthDate: String,
        genderCode: Int,
        countryId: Int?,
        cityId: Int?
    ): User = createMockUser()

    override suspend fun resetPassword(usernameOrEmail: String): LoginSuccess = createMockLoginSuccess()

    override suspend fun changePassword(
        password: String,
        newPassword: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun getUser(userId: Long): User = createMockUser(id = userId)

    override suspend fun editUser(
        userId: Long,
        name: RequestBody,
        fullName: RequestBody,
        email: RequestBody,
        birthDate: RequestBody?,
        gender: RequestBody?,
        countryId: RequestBody?,
        cityId: RequestBody?,
        image: MultipartBody.Part?
    ): User = createMockUser(id = userId)

    override suspend fun deleteUser(): Response<Unit> = Response.success(Unit)

    // ==================== ДРУЗЬЯ И ЧЕРНЫЙ СПИСОК ====================

    override suspend fun getFriendsForUser(userId: Long): List<User> =
        listOf(
            createMockUser(id = 2L, name = "friend1", fullName = "Friend One"),
            createMockUser(id = 3L, name = "friend2", fullName = "Friend Two")
        )

    override suspend fun getFriendRequests(): List<User> =
        listOf(
            createMockUser(id = 4L, name = "request1", fullName = "Request One")
        )

    override suspend fun acceptFriendRequest(userId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun declineFriendRequest(userId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun sendFriendRequest(userId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun deleteFriend(friendId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun getBlacklist(): List<User> =
        listOf(
            createMockUser(id = 5L, name = "blocked1", fullName = "Blocked User")
        )

    override suspend fun addToBlacklist(userId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun deleteFromBlacklist(userId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun findUsers(name: String): List<User> =
        listOf(
            createMockUser(id = 6L, name = name, fullName = "Found User")
        )

    // ==================== СТРАНЫ И ГОРОДА ====================

    override suspend fun getCountries(): List<Country> =
        listOf(
            createMockCountry(id = "1", name = "Russia"),
            createMockCountry(id = "2", name = "USA"),
            createMockCountry(id = "3", name = "Germany")
        )

    // ==================== ПЛОЩАДКИ ====================

    override suspend fun getAllParks(fields: String): List<Park> =
        listOf(
            createMockPark(id = 1L, name = "Park 1"),
            createMockPark(id = 2L, name = "Park 2"),
            createMockPark(id = 3L, name = "Park 3")
        )

    override suspend fun getUpdatedParks(date: String): List<Park> =
        listOf(
            createMockPark(id = 1L, name = "Updated Park")
        )

    override suspend fun getPark(parkId: Long): Park = createMockPark(id = parkId)

    override suspend fun createPark(
        address: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        cityId: RequestBody?,
        typeId: RequestBody,
        sizeId: RequestBody,
        photos: List<MultipartBody.Part>?
    ): Park = createMockPark(id = 100L, name = "New Park")

    override suspend fun editPark(
        parkId: Long,
        address: RequestBody,
        latitude: RequestBody,
        longitude: RequestBody,
        cityId: RequestBody?,
        typeId: RequestBody,
        sizeId: RequestBody,
        photos: List<MultipartBody.Part>?
    ): Park = createMockPark(id = parkId, name = "Edited Park")

    override suspend fun deletePark(parkId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun getParksForUser(userId: Long): List<Park> =
        listOf(
            createMockPark(id = 1L, name = "User Park 1"),
            createMockPark(id = 2L, name = "User Park 2")
        )

    override suspend fun postTrainHere(parkId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun deleteTrainHere(parkId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun addCommentToPark(
        parkId: Long,
        comment: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun editParkComment(
        parkId: Long,
        commentId: Long,
        comment: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun deleteParkComment(
        parkId: Long,
        commentId: Long
    ): Response<Unit> = Response.success(Unit)

    override suspend fun deleteParkPhoto(
        parkId: Long,
        photoId: Long
    ): Response<Unit> = Response.success(Unit)

    // ==================== МЕРОПРИЯТИЯ ====================

    override suspend fun getFutureEvents(): List<Event> =
        listOf(
            createMockEvent(id = 1L, title = "Future Event 1", beginDate = "2025-02-01"),
            createMockEvent(id = 2L, title = "Future Event 2", beginDate = "2025-03-01")
        )

    override suspend fun getPastEvents(): List<Event> =
        listOf(
            createMockEvent(id = 3L, title = "Past Event 1", beginDate = "2024-01-01"),
            createMockEvent(id = 4L, title = "Past Event 2", beginDate = "2024-02-01")
        )

    override suspend fun getEvent(eventId: Long): Event = createMockEvent(id = eventId)

    override suspend fun createEvent(
        title: RequestBody,
        description: RequestBody,
        date: RequestBody,
        parkId: RequestBody,
        photos: List<MultipartBody.Part>?
    ): Event = createMockEvent(id = 200L, title = "New Event")

    override suspend fun editEvent(
        eventId: Long,
        title: RequestBody,
        description: RequestBody,
        date: RequestBody,
        parkId: RequestBody,
        photos: List<MultipartBody.Part>?
    ): Event = createMockEvent(id = eventId, title = "Edited Event")

    override suspend fun postGoToEvent(eventId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun deleteGoToEvent(eventId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun addCommentToEvent(
        eventId: Long,
        comment: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun deleteEventComment(
        eventId: Long,
        commentId: Long
    ): Response<Unit> = Response.success(Unit)

    override suspend fun editEventComment(
        eventId: Long,
        commentId: Long,
        comment: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun deleteEvent(eventId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun deleteEventPhoto(
        eventId: Long,
        photoId: Long
    ): Response<Unit> = Response.success(Unit)

    // ==================== СООБЩЕНИЯ ====================

    override suspend fun getDialogs(): List<DialogResponse> =
        listOf(
            createMockDialog(id = 1L, userId = 2, name = "Dialog 1"),
            createMockDialog(id = 2L, userId = 3, name = "Dialog 2"),
            createMockDialog(id = 3L, userId = 4, name = "Dialog 3")
        )

    override suspend fun getMessages(dialogId: Long): List<MessageResponse> =
        listOf(
            createMockMessage(id = 1L, message = "Hello!", created = "2024-01-15T10:00:00"),
            createMockMessage(id = 2L, message = "How are you?", created = "2024-01-15T10:05:00")
        )

    override suspend fun sendMessageTo(
        userId: Long,
        message: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun markAsRead(fromUserId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun deleteDialog(dialogId: Long): Response<Unit> = Response.success(Unit)

    // ==================== ДНЕВНИКИ ====================

    override suspend fun getJournals(userId: Long): List<JournalResponse> =
        listOf(
            createMockJournal(id = 1L, title = "Journal 1"),
            createMockJournal(id = 2L, title = "Journal 2"),
            createMockJournal(id = 3L, title = "Journal 3")
        )

    override suspend fun getJournal(
        userId: Long,
        journalId: Long
    ): JournalResponse = createMockJournal(id = journalId)

    override suspend fun editJournalSettings(
        userId: Long,
        journalId: Long,
        title: String,
        viewAccess: String,
        commentAccess: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun createJournal(
        userId: Long,
        title: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun getJournalEntries(
        userId: Long,
        journalId: Long
    ): List<JournalEntryResponse> =
        listOf(
            createMockJournalEntry(id = 1L, message = "Entry 1"),
            createMockJournalEntry(id = 2L, message = "Entry 2"),
            createMockJournalEntry(id = 3L, message = "Entry 3")
        )

    override suspend fun saveJournalEntry(
        userId: Long,
        journalId: Long,
        message: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun editJournalEntry(
        userId: Long,
        journalId: Long,
        entryId: Long,
        newEntryText: String
    ): Response<Unit> = Response.success(Unit)

    override suspend fun deleteJournalEntry(
        userId: Long,
        journalId: Long,
        entryId: Long
    ): Response<Unit> = Response.success(Unit)

    override suspend fun deleteJournal(
        userId: Long,
        journalId: Long
    ): Response<Unit> = Response.success(Unit)
}

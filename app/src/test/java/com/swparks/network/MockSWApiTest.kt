package com.swparks.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSWApiTest {

    private val mockApi = MockSWApi()

    // ==================== Тесты вспомогательных методов создания фиктивных данных
    // ====================

    @Test
    fun createMockLoginSuccess_whenCalled_thenReturnsLoginSuccess() {
        // When
        val result = mockApi.createMockLoginSuccess(userId = 123L)

        // Then
        assertNotNull(result)
        assertEquals(123L, result.userId)
    }

    @Test
    fun createMockUser_whenCalledWithDefaults_thenReturnsUser() {
        // When
        val result = mockApi.createMockUser()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("testuser", result.name)
        assertEquals("Test User", result.fullName)
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun createMockPark_whenCalledWithDefaults_thenReturnsPark() {
        // When
        val result = mockApi.createMockPark()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test Park", result.name)
        assertEquals("Test Address", result.address)
        assertTrue(result.hasComments)
        assertTrue(result.hasPhotos)
    }

    @Test
    fun createMockEvent_whenCalledWithDefaults_thenReturnsEvent() {
        // When
        val result = mockApi.createMockEvent()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test Event", result.title)
        assertEquals("2024-02-01", result.beginDate)
    }

    @Test
    fun createMockDialog_whenCalledWithDefaults_thenReturnsDialog() {
        // When
        val result = mockApi.createMockDialog()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(2, result.anotherUserId)
        assertEquals("Test User", result.name)
    }

    @Test
    fun createMockMessage_whenCalledWithDefaults_thenReturnsMessage() {
        // When
        val result = mockApi.createMockMessage()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(2, result.userId)
        assertEquals("Test message", result.message)
    }

    @Test
    fun createMockJournal_whenCalledWithDefaults_thenReturnsJournal() {
        // When
        val result = mockApi.createMockJournal()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test Journal", result.title)
        assertEquals("Last entry", result.lastMessageText)
    }

    @Test
    fun createMockJournalEntry_whenCalledWithDefaults_thenReturnsJournalEntry() {
        // When
        val result = mockApi.createMockJournalEntry()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test entry", result.message)
    }

    @Test
    fun createMockComment_whenCalledWithDefaults_thenReturnsComment() {
        // When
        val result = mockApi.createMockComment()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test comment", result.body)
        assertNotNull(result.user)
    }

    @Test
    fun createMockPhoto_whenCalledWithDefaults_thenReturnsPhoto() {
        // When
        val result = mockApi.createMockPhoto()

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("https://example.com/photo.jpg", result.photo)
    }

    @Test
    fun createMockCountry_whenCalledWithDefaults_thenReturnsCountry() {
        // When
        val result = mockApi.createMockCountry()

        // Then
        assertNotNull(result)
        assertEquals("1", result.id)
        assertEquals("Russia", result.name)
    }

    // ==================== Тесты API методов ====================

    @Test
    fun login_whenCalled_thenReturnsMockLoginSuccess() = runTest {
        // When
        val result = mockApi.login()

        // Then
        assertNotNull(result)
        assertEquals(123L, result.userId)
    }

    @Test
    fun resetPassword_whenCalled_thenReturnsMockLoginSuccess() = runTest {
        // Given
        val usernameOrEmail = "test_login"

        // When
        val result = mockApi.resetPassword(usernameOrEmail)

        // Then
        assertNotNull(result)
        assertEquals(123L, result.userId)
    }

    @Test
    fun changePassword_whenCalled_thenReturnsSuccessResponse() = runTest {
        // Given
        val password = "old"
        val newPassword = "new"

        // When
        val result = mockApi.changePassword(password, newPassword)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getUser_whenCalled_thenReturnsMockUser() = runTest {
        // When
        val result = mockApi.getUser(123L)

        // Then
        assertNotNull(result)
        assertEquals(123L, result.id)
    }

    @Test
    fun deleteUser_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteUser()

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getFriendsForUser_whenCalled_thenReturnsMockFriends() = runTest {
        // When
        val result = mockApi.getFriendsForUser(1L)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("friend1", result[0].name)
    }

    @Test
    fun getFriendRequests_whenCalled_thenReturnsMockRequests() = runTest {
        // When
        val result = mockApi.getFriendRequests()

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("request1", result[0].name)
    }

    @Test
    fun acceptFriendRequest_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.acceptFriendRequest(2L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun declineFriendRequest_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.declineFriendRequest(2L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun sendFriendRequest_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.sendFriendRequest(2L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteFriend_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteFriend(2L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getBlacklist_whenCalled_thenReturnsMockBlacklist() = runTest {
        // When
        val result = mockApi.getBlacklist()

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("blocked1", result[0].name)
    }

    @Test
    fun addToBlacklist_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.addToBlacklist(2L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteFromBlacklist_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteFromBlacklist(2L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun findUsers_whenCalled_thenReturnsMockUsers() = runTest {
        // When
        val result = mockApi.findUsers("search_term")

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Found User", result[0].fullName)
    }

    @Test
    fun getCountries_whenCalled_thenReturnsMockCountries() = runTest {
        // When
        val result = mockApi.getCountries()

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Russia", result[0].name)
        assertEquals("USA", result[1].name)
        assertEquals("Germany", result[2].name)
    }

    @Test
    fun getAllParks_whenCalled_thenReturnsMockParks() = runTest {
        // When
        val result = mockApi.getAllParks()

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Park 1", result[0].name)
    }

    @Test
    fun getUpdatedParks_whenCalled_thenReturnsMockParks() = runTest {
        // When
        val result = mockApi.getUpdatedParks("2024-01-01")

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Updated Park", result[0].name)
    }

    @Test
    fun getPark_whenCalled_thenReturnsMockPark() = runTest {
        // When
        val result = mockApi.getPark(123L)

        // Then
        assertNotNull(result)
        assertEquals(123L, result.id)
    }

    @Test
    fun deletePark_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deletePark(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getParksForUser_whenCalled_thenReturnsMockParks() = runTest {
        // When
        val result = mockApi.getParksForUser(1L)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("User Park 1", result[0].name)
    }

    @Test
    fun postTrainHere_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.postTrainHere(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteTrainHere_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteTrainHere(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getFutureEvents_whenCalled_thenReturnsMockFutureEvents() = runTest {
        // When
        val result = mockApi.getFutureEvents()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Future Event 1", result[0].title)
    }

    @Test
    fun getPastEvents_whenCalled_thenReturnsMockPastEvents() = runTest {
        // When
        val result = mockApi.getPastEvents()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Past Event 1", result[0].title)
    }

    @Test
    fun getEvent_whenCalled_thenReturnsMockEvent() = runTest {
        // When
        val result = mockApi.getEvent(123L)

        // Then
        assertNotNull(result)
        assertEquals(123L, result.id)
    }

    @Test
    fun deleteEvent_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteEvent(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun postGoToEvent_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.postGoToEvent(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteGoToEvent_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteGoToEvent(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getDialogs_whenCalled_thenReturnsMockDialogs() = runTest {
        // When
        val result = mockApi.getDialogs()

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Dialog 1", result[0].name)
    }

    @Test
    fun getMessages_whenCalled_thenReturnsMockMessages() = runTest {
        // When
        val result = mockApi.getMessages(1L)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Hello!", result[0].message)
    }

    @Test
    fun sendMessageTo_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.sendMessageTo(2L, "Hello")

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun markAsRead_whenCalled_thenReturnsSuccessResponse() = runTest {
        // Given
        val fromUserId = 1L

        // When
        val result = mockApi.markAsRead(fromUserId)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteDialog_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteDialog(1L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getJournals_whenCalled_thenReturnsMockJournals() = runTest {
        // When
        val result = mockApi.getJournals(1L)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Journal 1", result[0].title)
    }

    @Test
    fun getJournal_whenCalled_thenReturnsMockJournal() = runTest {
        // When
        val result = mockApi.getJournal(1L, 123L)

        // Then
        assertNotNull(result)
        assertEquals(123L, result.id)
    }

    @Test
    fun editJournalSettings_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.editJournalSettings(
            userId = 1L,
            journalId = 123L,
            title = "New Title",
            viewAccess = "0",
            commentAccess = "0"
        )

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun createJournal_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.createJournal(1L, "New Journal")

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun getJournalEntries_whenCalled_thenReturnsMockEntries() = runTest {
        // When
        val result = mockApi.getJournalEntries(1L, 123L)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Entry 1", result[0].message)
    }

    @Test
    fun saveJournalEntry_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.saveJournalEntry(1L, 123L, "New entry")

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun editJournalEntry_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.editJournalEntry(1L, 123L, 456L, "Updated entry")

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteJournalEntry_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteJournalEntry(1L, 123L, 456L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }

    @Test
    fun deleteJournal_whenCalled_thenReturnsSuccessResponse() = runTest {
        // When
        val result = mockApi.deleteJournal(1L, 123L)

        // Then
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
    }
}

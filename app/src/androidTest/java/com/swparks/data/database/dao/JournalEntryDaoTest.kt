package com.swparks.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit тесты для JournalEntryDao
 *
 * Проверяет корректность работы DAO для записей в дневнике,
 * используя in-memory Room базу данных для изолированного тестирования
 */
@RunWith(AndroidJUnit4::class)
class JournalEntryDaoTest {

    private lateinit var db: SWDatabase
    private lateinit var journalEntryDao: JournalEntryDao

    private val testJournalId1 = 1L
    private val testJournalId2 = 2L

    @Before
    fun setup() {
        // Создаем in-memory базу данных для тестирования
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SWDatabase::class.java
        ).build()
        journalEntryDao = db.journalEntryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Вспомогательная функция для создания тестовой записи
     */
    private fun createTestJournalEntryEntity(
        id: Long = 1L,
        journalId: Long = testJournalId1,
        authorId: Long? = 100L,
        authorName: String? = "Test Author",
        message: String? = "Test message",
        createDate: String? = "2024-01-01T10:00:00",
        modifyDate: Long = 1704110400000L,
        authorImage: String? = "https://example.com/image.jpg"
    ): JournalEntryEntity {
        return JournalEntryEntity(
            id = id,
            journalId = journalId,
            authorId = authorId,
            authorName = authorName,
            message = message,
            createDate = createDate,
            modifyDate = modifyDate,
            authorImage = authorImage
        )
    }

    /**
     * Тест 1: Проверка возврата пустого списка для отсутствующих записей
     */
    @Test
    fun testGetJournalEntriesByJournalId_returnsEmpty() = runTest {
        // When - получаем записи для несуществующего дневника
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()

        // Then - должен вернуться пустой список
        assertTrue(entries.isEmpty())
    }

    /**
     * Тест 2: Проверка возврата списка записей
     */
    @Test
    fun testGetJournalEntriesByJournalId_returnsEntries() = runTest {
        // Given - вставляем записи в БД
        val testEntries = listOf(
            createTestJournalEntryEntity(id = 1L),
            createTestJournalEntryEntity(id = 2L),
            createTestJournalEntryEntity(id = 3L)
        )
        journalEntryDao.insertAll(testEntries)

        // When - получаем записи
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()

        // Then - должны вернуться все три записи
        assertEquals(3, entries.size)
        assertTrue(entries.isNotEmpty())
    }

    /**
     * Тест 3: Проверка сортировки по modifyDate DESC
     */
    @Test
    fun testGetJournalEntriesByJournalId_ordersByModifyDateDesc() = runTest {
        // Given - вставляем записи с разными датами изменения
        val entriesWithDifferentDates = listOf(
            createTestJournalEntryEntity(id = 1L, modifyDate = 1704110400000L), // 2024-01-01
            createTestJournalEntryEntity(id = 2L, modifyDate = 1704196800000L), // 2024-01-02
            createTestJournalEntryEntity(id = 3L, modifyDate = 1704283200000L)  // 2024-01-03
        )
        journalEntryDao.insertAll(entriesWithDifferentDates)

        // When - получаем записи
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()

        // Then - записи должны быть отсортированы по modifyDate DESC (от новых к старым)
        assertEquals(3, entries.size)
        // Первая запись должна быть с самой большой датой
        assertTrue(entries[0].modifyDate >= entries[1].modifyDate)
        assertTrue(entries[1].modifyDate >= entries[2].modifyDate)
        // Проверяем конкретный порядок
        assertEquals(3L, entries[0].id) // Самая новая
        assertEquals(2L, entries[1].id)
        assertEquals(1L, entries[2].id) // Самая старая
    }

    /**
     * Тест 4: Проверка вставки записей
     */
    @Test
    fun testInsertAll_insertsEntries() = runTest {
        // Given - создаем тестовые записи
        val testEntries = listOf(
            createTestJournalEntryEntity(id = 1L, message = "Message 1"),
            createTestJournalEntryEntity(id = 2L, message = "Message 2")
        )

        // When - вставляем записи
        journalEntryDao.insertAll(testEntries)

        // Then - записи должны быть успешно вставлены
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        assertEquals(2, entries.size)
        assertEquals("Message 1", entries[0].message)
        assertEquals("Message 2", entries[1].message)
    }

    /**
     * Тест 5: Проверка обновления при дубликатах (onConflict = REPLACE)
     */
    @Test
    fun testInsertAll_upsertsEntries() = runTest {
        // Given - вставляем начальную запись
        val initialEntry = createTestJournalEntryEntity(
            id = 1L,
            message = "Original message",
            modifyDate = 1704110400000L
        )
        journalEntryDao.insertAll(listOf(initialEntry))

        // When - вставляем запись с тем же ID (REPLACE должен обновить)
        val updatedEntry = createTestJournalEntryEntity(
            id = 1L,
            message = "Updated message",
            modifyDate = 1704196800000L
        )
        journalEntryDao.insertAll(listOf(updatedEntry))

        // Then - запись должна быть обновлена
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        assertEquals(1, entries.size)
        assertEquals("Updated message", entries[0].message)
        assertEquals(1704196800000L, entries[0].modifyDate)
    }

    /**
     * Тест 6: Проверка удаления всех записей дневника
     */
    @Test
    fun testDeleteByJournalId_deletesAll() = runTest {
        // Given - вставляем записи
        val testEntries = listOf(
            createTestJournalEntryEntity(id = 1L),
            createTestJournalEntryEntity(id = 2L),
            createTestJournalEntryEntity(id = 3L)
        )
        journalEntryDao.insertAll(testEntries)

        // When - удаляем записи по journalId
        journalEntryDao.deleteByJournalId(testJournalId1)

        // Then - все записи должны быть удалены
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        assertTrue(entries.isEmpty())
    }

    /**
     * Тест 7: Проверка, что удаляются только записи нужного дневника
     */
    @Test
    fun testDeleteByJournalId_doesNotDeleteOtherJournals() = runTest {
        // Given - вставляем записи для двух разных дневников
        val journal1Entries = listOf(
            createTestJournalEntryEntity(id = 1L, journalId = testJournalId1),
            createTestJournalEntryEntity(id = 2L, journalId = testJournalId1)
        )
        val journal2Entries = listOf(
            createTestJournalEntryEntity(id = 3L, journalId = testJournalId2),
            createTestJournalEntryEntity(id = 4L, journalId = testJournalId2)
        )
        journalEntryDao.insertAll(journal1Entries)
        journalEntryDao.insertAll(journal2Entries)

        // When - удаляем записи только первого дневника
        journalEntryDao.deleteByJournalId(testJournalId1)

        // Then - записи первого дневника должны быть удалены, второго - нет
        val journal1Result = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        val journal2Result = journalEntryDao.getJournalEntriesByJournalId(testJournalId2).first()

        assertTrue(journal1Result.isEmpty())
        assertEquals(2, journal2Result.size)
    }

    /**
     * Тест 8: Проверка, что Flow эммитит изменения
     */
    @Test
    fun testGetJournalEntriesByJournalId_emitsUpdates() = runTest {
        // Given - создаем Flow для наблюдения
        val entriesFlow = journalEntryDao.getJournalEntriesByJournalId(testJournalId1)

        // When 1 - получаем начальное состояние (пустой список)
        val initialEntries = entriesFlow.first()
        assertEquals(0, initialEntries.size)

        // When 2 - вставляем первую запись
        val firstEntry = createTestJournalEntryEntity(id = 1L, message = "First message")
        journalEntryDao.insertAll(listOf(firstEntry))
        val afterFirstInsert = entriesFlow.first()
        assertEquals(1, afterFirstInsert.size)
        assertEquals("First message", afterFirstInsert[0].message)

        // When 3 - вставляем вторую запись
        val secondEntry = createTestJournalEntryEntity(id = 2L, message = "Second message")
        journalEntryDao.insertAll(listOf(secondEntry))
        val afterSecondInsert = entriesFlow.first()
        assertEquals(2, afterSecondInsert.size)

        // When 4 - удаляем все записи
        journalEntryDao.deleteByJournalId(testJournalId1)
        val afterDelete = entriesFlow.first()
        assertEquals(0, afterDelete.size)
    }

    /**
     * Тест 9: Проверка удаления записи по ID
     */
    @Test
    fun testDeleteById_deletesEntry() = runTest {
        // Given - вставляем несколько записей
        val testEntries = listOf(
            createTestJournalEntryEntity(id = 1L, message = "Entry 1"),
            createTestJournalEntryEntity(id = 2L, message = "Entry 2"),
            createTestJournalEntryEntity(id = 3L, message = "Entry 3")
        )
        journalEntryDao.insertAll(testEntries)

        // When - удаляем запись с id = 2
        journalEntryDao.deleteById(2L)

        // Then - запись с id = 2 должна быть удалена, остальные - нет
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        assertEquals(2, entries.size)
        assertEquals(1L, entries[0].id)
        assertEquals(3L, entries[1].id)
    }

    /**
     * Тест 10: Проверка, что deleteById влияет только на указанный ID
     */
    @Test
    fun testDeleteById_doesNotDeleteOtherEntries() = runTest {
        // Given - вставляем записи
        val testEntries = listOf(
            createTestJournalEntryEntity(id = 1L),
            createTestJournalEntryEntity(id = 2L),
            createTestJournalEntryEntity(id = 3L)
        )
        journalEntryDao.insertAll(testEntries)

        // When - удаляем запись с id = 1
        journalEntryDao.deleteById(1L)

        // Then - остальные записи должны остаться
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.id != 1L })
    }

    /**
     * Тест 11: Проверка удаления несуществующей записи
     */
    @Test
    fun testDeleteById_nonExistentEntry() = runTest {
        // Given - вставляем записи
        val testEntries = listOf(
            createTestJournalEntryEntity(id = 1L),
            createTestJournalEntryEntity(id = 2L)
        )
        journalEntryDao.insertAll(testEntries)

        // When - пытаемся удалить несуществующую запись
        journalEntryDao.deleteById(999L)

        // Then - существующие записи должны остаться без изменений
        val entries = journalEntryDao.getJournalEntriesByJournalId(testJournalId1).first()
        assertEquals(2, entries.size)
    }
}

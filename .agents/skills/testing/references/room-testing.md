# Room in-memory testing

DAO-тесты в обоих source set:

- **Robolectric** в `app/src/test/.../database/dao/` — `ParkDaoTest.kt`,
  `UserTrainingParkDaoTest.kt` (`@RunWith(RobolectricTestRunner)`,
  `inMemoryDatabaseBuilder` в JVM).
- **Реальный Room** в `app/src/androidTest/.../database/dao/` —
  `JournalEntryDaoTest.kt` (нужен Android Context для in-memory Room
  через `ApplicationProvider`; Keystore относится к
  `CryptoManagerIntegrationTest`, не к DAO).

Два каркаса ниже: «Каркас 1» — для androidTest, «Каркас 2» — для unit + Robolectric.

## Каркас 1: in-memory (androidTest)

```kotlin
@RunWith(AndroidJUnit4::class)
class JournalEntryDaoTest : TimeoutTest(180) {       // 180 с для Keystore/Room
    private lateinit var db: SWDatabase
    private lateinit var journalEntryDao: JournalEntryDao

    @Before
    fun setup() {
        db = Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                SWDatabase::class.java
            )
            .build()
        journalEntryDao = db.journalEntryDao()
    }

    @After
    fun tearDown() { db.close() }
}
```

`TimeoutTest(180)` доступен только в `androidTest`
(`app/src/androidTest/java/com/swparks/testing/TimeoutTest.kt`,
пакет `com.swparks.testing`); в `test/` он недоступен. Для Keystore/Tink
и Room in-memory не-UI тестов — `TimeoutTest(180)`. UI-наследники —
`TimeoutTest()` (60 с). Robolectric-DAO в `test/` — без `TimeoutTest`
(JUnit-default timeout); см. «Каркас 2».

## Каркас 2: Robolectric (test/)

Из `ParkDaoTest.kt`:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParkDaoTest {
    private lateinit var db: SWDatabase
    private lateinit var parkDao: ParkDao

    @Before
    fun setup() {
        db = Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                SWDatabase::class.java
            )
            .allowMainThreadQueries()             // <-- обязателен в Robolectric
            .build()
        parkDao = db.parkDao()
    }

    @After
    fun tearDown() { db.close() }
}
```

См. «Питфоллы» ниже для обеих source set.

## Чтение/запись

Пример ниже — на `JournalEntryDao` (androidTest, `JournalEntryDaoTest`).
В Robolectric-каркасе — те же вызовы на `parkDao` под `runTest`.

```kotlin
@Test
fun insertAndGetById_whenEntityInserted_thenReturnsInserted() = runTest {
    // Given
    val entity = JournalEntryEntity(
        id = 1L,
        journalId = 10L,
        authorId = 100L,
        message = "Hello"
    )

    // When
    journalEntryDao.insert(entity)
    val loaded = journalEntryDao.getById(1L)

    // Then
    assertNotNull("Запись должна загрузиться", loaded)
    assertEquals(entity.message, loaded?.message)
}
```

## Flow из DAO

Тот же source set, что и предыдущий раздел (androidTest для
`JournalEntryDao`, Robolectric — для `parkDao`).

```kotlin
@Test
fun observeAll_whenRecordsChange_thenEmitUpdated() = runTest {
    // Given
    val inserted = listOf(entity1, entity2)
    inserted.forEach { journalEntryDao.insert(it) }

    // When
    val first = journalEntryDao.observeAll().first()

    // Then
    assertEquals(2, first.size)

    journalEntryDao.delete(entity1)
    val updated = journalEntryDao.observeAll().first()
    assertEquals(1, updated.size)
}
```

## Питфолы

- **«cannot access database on the main thread»** — в Robolectric-DAO
  (`test/.../database/dao/`, `ParkDaoTest`, `UserTrainingParkDaoTest`)
  флаг `.allowMainThreadQueries()` обязателен. В androidTest
  (`JournalEntryDaoTest`) DAO вызывается внутри `runTest` — флаг не
  нужен; используется только если DAO вызывается с main-потока вне `runTest`.
- **Cleanup данных не нужен** для per-test базы (`inMemoryDatabaseBuilder`
  создаёт отдельный instance на каждый `@Before`).
- **`process-wide` база через `getDatabase()`** — обязателен
  `.clearAllTables()` в `@After`, иначе состояние утекает между тестами.

## Repository, использующий DAO

**androidTest**: реальный `DAO` создаётся в `setup()` каркаса выше и
передаётся в `*RepositoryImpl` через конструктор напрямую
(без DI-графа):

```kotlin
private lateinit var journalEntryRepository: JournalEntryRepository

@Before
fun setup() {
    db = Room.inMemoryDatabaseBuilder(...).build()
    journalEntryRepository = JournalEntryRepositoryImpl(db.journalEntryDao())
}

@Test
fun insert_whenCalled_thenRepositoryPersists() = runTest {
    // Given
    val entity = JournalEntryEntity(...)

    // When
    journalEntryRepository.save(entity)

    // Then
    assertNotNull("Запись должна загрузиться", journalEntryRepository.getById(entity.id))
}
```

**Unit-test** слоя репозитория — `Fake DAO` на `MutableStateFlow`,
подкладывается в конструктор напрямую (см.
`references/fakes-and-stateflow.md`, раздел «Fake*Repository в unit»).
DAO в unit-тесте вызывается через `runTest` (правило `MainDispatcherRule`
см. в `references/viewmodel-testing.md`).

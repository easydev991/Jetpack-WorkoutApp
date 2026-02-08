---
name: testing
description: Write tests correctly in Android projects. Use this skill when writing any type of tests (unit, integration, UI), testing business logic, network functions, and UI components.
---

# Testing

## When to Use

- Use this skill when writing unit tests for business logic (ViewModels, Use Cases, Domain models)
- Use this skill when writing integration tests for DAO and Repository
- Use this skill when writing UI tests for Compose components
- Use this skill when testing network functions (only on mocks)
- Use this skill when testing Flow/StateFlow using Turbine
- Use this skill when you need to test exception handling in coroutines
- This skill is useful when choosing the right type of test for a specific scenario
- This skill helps determine when to use unit tests with mocks vs integration tests

## Test Types

- **Unit**: business logic in isolation, MockK for dependencies, AAA pattern
- **Integration**: interaction between layers (DAO, Repository), real DB implementations
- **UI**: critical scenarios, Compose Testing for components

## Tools

- JUnit 5 - unit tests
- MockK - mocking
- Compose Testing - Compose components
- Room Testing - for DB integration tests
- kotlinx-coroutines-test - for coroutine testing
- Turbine - for Flow/StateFlow testing (app.cash.turbine:turbine:1.1.0)

## Running Tests and Reports

### make test command

Use the `make test` command to run all unit tests:

```bash
make test
```

This command:

1. Runs `./gradlew test --console=plain` - all unit tests (JVM, without device)
2. Automatically executes `scripts/test_report.py` script after test completion
3. Shows detailed report with statistics by test classes

### test_report.py script

The `scripts/test_report.py` script generates a detailed test results report:

- Shows overall statistics: total tests, passed, failed
- Lists all failed tests with class and method names
- Displays statistics table by test classes (total, failed, passed)
- Sorts classes by number of failed tests (descending)
- Uses colors for easy reading (green for success, red for errors)
- Returns exit code 0 if all tests passed, 1 if there are failures

Example script output:

```
================================================================================
✅ BUILD SUCCESSFUL
================================================================================

Test Statistics:
Total tests: 145
✅ Passed: 142
❌ Failed: 3
❌ List of failed tests:
  - AuthViewModelTest::login_withInvalidCredentials_returnsError
  - EventsViewModelTest::loadEvents_whenNetworkError_returnsError
  - ParksRepositoryImplTest::getParks_whenEmpty_returnsEmptyList

================================================================================
Statistics by test classes (5 classes):
================================================================================
Class                                          Failed  Passed  Total
----------------------------------------------------------------------------
AuthViewModelTest                                  1      12       13
EventsViewModelTest                                1       8        9
ParksRepositoryImplTest                            1      15       16
================================================================================
```

### Other Testing Commands

- `make android-test` - run integration tests on Android device
- `make test-all` - run all tests (unit + integration)
- `make android-test-report` - open HTML integration test report in browser

## Structure

- `app/src/test/` - unit tests (ViewModels, Use Cases, Domain models)
- `app/src/androidTest/` - integration/UI tests (DAO, Repository, UI components)
- Structure mirrors code
- Class names: `*Test`

## Best Practices

**Important:** Recommendations for ViewModel integration tests

- **Recommended:** Test ViewModels via unit tests with MockK in test/
- **Possible (but not recommended):** Integration tests for ViewModels in androidTest/ using `runTest`, `MainDispatcherRule`, and `Turbine`
- **Acceptable:** Integration tests for components with Android API (e.g., CryptoManager with Android Keystore)
- **Acceptable:** UI tests for Compose components without business logic

**Why unit tests with MockK are preferred:**

- Faster execution (no real Repository/DB needed)
- More stable and isolated
- In Jetpack-WorkoutApp all ViewModels are tested via unit tests (AuthViewModelTest, EventsViewModelTest)

**When to use ViewModel integration tests:**

- Only if you need to test integration with real Repository/DB
- Not currently used in JetpackWorkoutApp, but technically possible

### Working Testing Approach

#### Unit Tests for ViewModels (with MockK)

**Important:** ViewModels using `viewModelScope.launch` require `MainDispatcherRule` in tests. `viewModelScope` runs on `Dispatchers.Main`, which is not configured by default in Unit tests. Without `MainDispatcherRule`, coroutines won't be able to start, and state checks will be incorrect.

**Creating `MainDispatcherRule`:**

```kotlin
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

**Example unit test:**

```kotlin
@ExperimentalCoroutinesApi
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: AuthViewModel
    private val loginUseCase: LoginUseCase = mockk()

    @Before
    fun setup() {
        viewModel = AuthViewModel(loginUseCase)
    }

    @Test
    fun login_whenValidCredentials_thenReturnsSuccess() = runTest {
        // Given
        val credentials = LoginCredentials("user@test.com", "password")
        coEvery { loginUseCase(credentials) } returns Result.success(loginSuccess)

        // When
        viewModel.login()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value is LoginUiState.Idle)
    }
}
```

#### Integration Tests for DAO and Repository

Use real in-memory database for DAO tests:

```kotlin
@RunWith(AndroidJUnit4::class)
class ParksDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var parksDao: ParksDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        parksDao = database.parksDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetPark() = runTest {
        // Given
        val park = ParkEntity(/* ... */)

        // When
        parksDao.insert(park)
        val loaded = parksDao.getById(park.id)

        // Then
        assertEquals(park, loaded)
    }
}
```

#### UI Tests for Compose Components

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_showsEmailField() {
        composeTestRule.setContent {
            LoginScreen()
        }

        composeTestRule
            .onNodeWithText("Email")
            .assertIsDisplayed()
    }
}
```

### Testing Network Functions

- **Only on mocks**: Use MockK for API clients (SWApi, Retrofit)
- **No real requests**: Real HTTP requests to server are prohibited in tests
- **Isolated testing**: Test business logic independently of network
- **Predictable responses**: Use fixed mock server responses

**Mock example:**

```kotlin
@Test
fun loadParks_whenSuccess_thenReturnsParks() = runTest {
    // Given
    val mockParks = listOf(Park(/* ... */))
    coEvery { api.getParks() } returns Response.success(mockParks)

    // When
    val result = repository.getParks()

    // Then
    assertEquals(mockParks, result)
}
```

**Mocking HttpException:**

```kotlin
@Test
fun loadParks_whenNetworkError_thenReturnsError() = runTest {
    // Given
    val httpException = HttpException(
        Response.error<Any>(404, "Not found".toResponseBody())
    )
    coEvery { api.getParks() } throws httpException

    // When
    val result = repository.getParks()

    // Then
    assertTrue(result.isFailure)
}
```

### Testing Flow with Exceptions

**Important:** For Flows with exceptions handled via `catch`, use `first()` or `collect()` instead of Turbine.

**Testing IOException (handled in catch):**

```kotlin
@Test
fun loadData_whenIOException_thenReturnsErrorState() = runTest {
    // Given
    coEvery { repository.getData() } throws IOException("Network error")

    // When
    val result = useCase().first()

    // Then
    assertTrue(result.isFailure)
}
```

**Testing other exceptions (propagated):**

```kotlin
@Test(expected = IllegalStateException::class)
fun loadData_whenIllegalState_thenThrows() = runTest {
    // Given
    coEvery { repository.getData() } throws IllegalStateException()

    // When
    useCase().first()
}
```

**Mocking Android Log:**

```kotlin
@Before
fun setup() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
}
```

### General Practices

- Fast and independent tests
- Descriptive names
- One test - one assertion
- Test behavior, not implementation
- Integration tests only for DAO and Repository
- Unit tests for ViewModels with mocks
- Network functions only on mocks (no real requests)
- UI tests for Compose components without business logic
- Use JUnit 5 annotations (`@Test`, `@Before`, `@After`)
- Use JUnit 5 assertions (`assertEquals`, `assertTrue`, `assertNull`)

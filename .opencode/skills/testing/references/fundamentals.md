# Fundamentals

Анатомия unit и androidTest в проекте SW Parks.

## JUnit 4

Во всём проекте — только JUnit 4. Импорты из `org.junit.*`:

```kotlin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
```

**Запрещено**: `org.junit.jupiter.api.*` (JUnit 5 — не наш стек).

## Имена тестов

Формат: **`functionName_whenCondition_thenExpectedResult()`** — camelCase,
без обратных кавычек. Допускаются вариации:

- `subject_whenCondition_thenResult`
- `subject_verbSuffix_whenCondition_thenResult`
- `verb_whenCondition_thenResult` (для use-case-стиля `invoke_*`)

Имя класса оканчивается на `Test` (в нашем проекте встречаются также
`IntegrationTest`, например `CryptoManagerIntegrationTest`).

## Robolectric

Для unit-тестов с Android `Context` / `Resources`
(`SharedPreferences`, `Locale`, строки из ресурсов) — аннотация
`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [...])`.

## Given / When / Then

Три маркера-комментария в каждом тесте — `// Given`, `// When`,
`// Then`. Допускается опустить `// Given`, если входных данных нет
(например, в тестах, состоящих только из действия над начальным
состоянием). Пример из реального проекта:

```kotlin
@Test
fun invoke_whenValidCredentials_thenSavesTokenAndCallsLogin() = runTest {
    // Given
    coEvery { authRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))

    // When
    val result = loginUseCase(testCredentials)

    // Then
    assertTrue(result.isSuccess)
    coVerify { tokenEncoder.encode(testCredentials) }
}
```

## Параметризация

Канон проекта — **`forEach` внутри `@Test`**, без
`@RunWith(Parameterized::class)` (в проекте 0 таких — не вводить без
реальной нужды: отдельный runner = отдельный инстанс класса на каждый
кейс и медленнее прогон).

Пример из реального проекта (`MessagesRepositoryImplTest`):

```kotlin
@Test
fun markDialogAsRead_whenApiReturnsAnyResponse_thenResetsUnreadCountAndReturnsSuccess() = runTest {
    // Given
    val responses = listOf(
        Response.success(Unit),
        Response.error(403, "{}".toResponseBody(null))
    )

    responses.forEach { response ->
        val mockApi = mockk<SWApi>()
        val dialogDao = mockk<DialogDao>(relaxed = true)
        coEvery { mockApi.markAsRead(2L) } returns response
        val repository = createRepository(mockApi, dialogDao)

        // When
        val result = repository.markDialogAsRead(dialogId = 1L, userId = 2)

        // Then
        assertTrue("Ожидался success при ответе $response", result.isSuccess)
        coVerify(exactly = 1) { dialogDao.updateUnreadCount(0) }
    }
}
```

Три правила:

1. **Моки, по которым `verify(exactly = N)`, создавать внутри
   итерации.** Общий мок накапливает вызовы через итерации —
   `exactly = 1` упадёт на второй. `relaxed = true` + локальное
   создание — дёшево и изолированно.
2. `verify`/`coVerify` — внутри итерации, рядом с assert.
3. В имени теста — обобщение (`whenApiReturnsAnyResponse`), конкретные
   значения — в списке. Если кейсы ведут себя по-разному (разный
   результат) — это отдельные тесты, а не параметризация.

## Сообщения assert на русском

С контекстом и фактическими значениями:

```kotlin
assertEquals(
    "Ожидалось сохранённое имя пользователя",
    "TestUser",
    preferencesRepository.getUserName()
)
```

## Зеркалирование структуры

`app/src/main/.../foo/Bar.kt` → тесты:

- Unit: `app/src/test/.../foo/BarTest.kt`
- androidTest: `app/src/androidTest/.../foo/BarTest.kt`

Класс оканчивается на `Test`.

## Запрет `!!`

В тестах — то же правило, что в проде. `?.`, `?:`, `let`, `checkNotNull`,
`assertNotNull(...)` перед доступом к полям.

## Порядок работы

Канон для нового экрана/фичи — **TDD (tests → logic → UI)**:

1. **Tests first** — `Fake*ViewModel` в `androidTest/.../ui/viewmodel/`
   (если ещё нет), компонентный UI-тест с пустым экраном.
2. **Logic** — Use Case → ViewModel с реальным DI, проверяем через
   `runTest` + `MainDispatcherRule` + `advanceUntilIdle` в
   `test/.../viewmodel/`.
3. **UI** — добавить ноды/экраны в `setContent` и селекторы; UI-тест
   должен оставаться зелёным без изменений в нодах до перехода на
   следующий шаг.

Если фича без реального UI (data-layer, Use Case, валидатор) — шаг 3
пропускается.

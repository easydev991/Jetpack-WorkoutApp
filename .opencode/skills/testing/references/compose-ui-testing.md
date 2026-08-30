# Compose UI testing

Канонические паттерны для `app/src/androidTest/`. Все UI-классы
наследуют `TimeoutTest()` (60 с).

## Два типа Compose-правил

- **`createComposeRule()`** (импорт `androidx.compose.ui.test.junit4.v2.createComposeRule`)
  — изолированный Compose-компонент (зависимости
  подменяются `Fake*ViewModel`). Используется в большинстве наших
  ScreenTest-ов (компонентные тесты).
- **`createAndroidComposeRule<MainActivity>()`** — целый экран внутри
  реальной `MainActivity` с полным DI-графом. **В проекте 0 файлов**,
  не используется.

> **Один `setContent` на тест.** Повторный бросает
> `IllegalStateException: ... has already set content`. Смена экрана —
> через `mutableStateOf` внутри первого `setContent` (паттерн
> `ThemeIconScreenTest`).

## Канонический компонентный тест

Из `LoginScreenTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginScreenTest : TimeoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val passwordText = context.getString(R.string.password)   // R.string.* — см. «Строки UI»

    private fun setContent(
        viewModel: FakeLoginViewModel = FakeLoginViewModel(),         // Fake*ViewModel — см. «Fake*ViewModel»
        onDismiss: () -> Unit = {},
        onLoginSuccess: (userId: Long) -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onDismiss = onDismiss,
                    onLoginSuccess = onLoginSuccess
                )
            }
        }
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsTitle() {
        // When
        setContent()

        // Then
        composeTestRule.onNodeWithTag("loginTitle").assertIsDisplayed()
    }
}
```

## Селекторы

- `onNodeWithText("...")` — точный текст.
- `onNodeWithText(text, substring = true)` — подстрока.
- `onNodeWithTag("myTag")` — `Modifier.testTag("myTag")` в проде.
- `onNodeWithContentDescription("...")`.
- `onAllNodesWithText("...")` — список, **обязательно `assertCountEquals(n)`** —
  иначе тест проходит при 0 нод.

## Действия

- `performClick()`, `performTextInput("...")`, `performTextClearance()`.
- Для swipe/scroll — `performTouchInput { swipe(...) }`.
- На **disabled-ноде** `performClick` падает — у неё нет click-action.
  Паттерн «тап по недоступному → ничего не произошло» не работает.
  Проверяй `assertIsNotEnabled`.

## Assert

- `assertIsDisplayed()` / `assertIsNotDisplayed()`.
- `assertIsEnabled()` / `assertIsNotEnabled()`.
- `assertTextEquals("...")`.
- `assertCountEquals(n)`.

## Merged-контейнеры

Строки списка, кнопки, диалоги используют `mergeDescendants = true` —
внутренние ноды не видны. Ищи с `useUnmergedTree = true`:

```kotlin
composeTestRule.onNodeWithTag("listItem", useUnmergedTree = true).performClick()
```

## Ожидание данных

**`waitForIdle()`** — после `runBlocking { db.insert(...) }` иначе UI не
перерисуется.

**`waitUntil(timeoutMillis = 5000) { ... }`** — условное ожидание
(без `Thread.sleep`). Позиционный `5000` уже используется в 17 местах
— переход на именованный `timeoutMillis = 5000` по усмотрению автора.

## Строки UI

Только через ресурсы:

```kotlin
val context = InstrumentationRegistry.getInstrumentation().targetContext
val saveText = context.getString(R.string.save)
composeTestRule.onNodeWithText(saveText).performClick()
```

## Fake*ViewModel

Каждый экранный тест подменяет реальный ViewModel на `Fake*ViewModel`
из `ui/viewmodel/`. Конвенция синхронизации Fake ↔ реальный ViewModel —
`references/fakes-and-stateflow.md` (раздел «Fake*ViewModel в androidTest»).

## Сетевые тесты

Сеть AVD нестабильна (`wpa_supplicant: BEACON_LOSS`). Сеть-зависимые
тесты — через перехват Retrofit через `OkHttpInterceptor`. **MockWebServer
не используется** (нет зависимости в проекте, OkHttpInterceptor
достаточен).

# Tasks

## 1. Red — падающие тесты

- [ ] 1.1 Добавить `FakeIOtherUserProfileViewModel` в
      `app/src/androidTest/java/com/swparks/ui/screens/profile/OtherUserProfileScreenTest.kt`
      (по образцу `FakeProfileViewModel` из
      `app/src/androidTest/java/com/swparks/ui/viewmodel/FakeProfileViewModel.kt`,
      используемого в `ProfileRootScreenTest`: настраиваемые
      `uiState`/`viewedUser`/`currentUser`/`blacklist`; остальные 4 поля
      (`friends`, `isRefreshing`, `isLoadingCurrentUser`, `isFriendActionLoading`) — со
      значениями по умолчанию; заглушки методов `IOtherUserProfileViewModel`). Перед
      прогоном поднять эмулятор и выполнить `make emulator-fast` (AGENTS.md). Проверка:
      класс компилируется, существующие тесты
      файла проходят (`./gradlew :app:connectedDebugAndroidTest
      -Pandroid.testInstrumentationRunnerArguments.class=com.swparks.ui.screens.profile.OtherUserProfileScreenTest`).
- [ ] 1.2 Тест `otherUserProfileScreen_whenErrorState_thenHidesBlacklistButton`:
      контент `OtherUserProfileScreen(viewModel = fake, appState = rememberAppState(
      analyticsService = <заглушка/mockk AnalyticsService>))` — `appState` обязателен
      без дефолта, `navController` берётся из дефолта `rememberAppState()`;
      `uiState = OtherUserProfileUiState.Error(...)`, `viewedUser = null`;
      `onNodeWithContentDescription(R.string.block).assertDoesNotExist()`, заголовок
      (`R.string.profile`) и кнопка закрытия (`R.string.close_button_content_description`)
      — `assertIsDisplayed()`. Проверка: тест падает — кнопка есть
      (подтверждение диагноза).
- [ ] 1.3 Контрольный тест `otherUserProfileScreen_whenSuccessState_thenShowsBlacklistButton`
      (тот же setContent, что в 1.2): `uiState = Success(...)`, `viewedUser` с id ≠ id
      `currentUser` (guard на свой
      профиль, см. design.md); кнопка блокировки видна, заголовок (`R.string.profile`) и
      кнопка закрытия (`R.string.close_button_content_description`) —
      `assertIsDisplayed()` (спека: «Верхняя панель при загруженном профиле»). Проверка:
      тест проходит уже в Red — кнопка видна.

## 2. Green — реализация

- [ ] 2.1 В `OtherUserProfileScaffold`
      (`app/src/main/java/com/swparks/ui/screens/profile/OtherUserProfileScreen.kt`)
      вычислить видимость кнопки (`params.uiState is Success` — поле уже есть в
      `OtherUserProfileScaffoldParams`, новый проброс не нужен) и передать параметром в
      `OtherUserProfileTopAppBar`; слот `actions` рендерить только при видимости,
      кнопку закрытия и заголовок не менять. Проверка: тесты 1.2 и 1.3 зелёные (прогон из 1.1).

## 3. Верификация

- [ ] 3.1 `make format && make lint && make test` — зелёные, без новых предупреждений
      detekt (лимит не повышать); затем `make build` (pre-commit-чеклист AGENTS.md).
      Проверка: все команды завершаются без ошибок.
- [ ] 3.2 Полный прогон UI-тестов (`make emulator-fast`, затем `make android-test`) —
      все UI-тесты зелёные. Проверка: `make android-test` без падений.
- [ ] 3.3 (опционально, не блокирует) Ручная проверка на эмуляторе (`make install`):
      профиль с ошибкой → кнопки блокировки нет; после retry (Success) → кнопка есть и
      открывает диалог. Проверка: визуальный осмотр; автоматическое покрытие — п. 3.2.

## Workflow follow-up

- Архивировать change после проверки по требованиям проекта (`/opsx-archive`).

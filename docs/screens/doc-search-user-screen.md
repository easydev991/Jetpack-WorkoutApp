# План: Экран поиска пользователей (SearchUserScreen)

## Статус: ✅ ЗАВЕРШЕНО

Экран поиска пользователей по никнейму. В текущей навигации открывается из `ProfileTopAppBar` и из `MessagesRootScreen`.

**Реализовано:**
- Поиск по никнейму (минимум 2 символа, автотрим пробелов)
- Валидация и предотвращение повторных запросов
- `LoadingOverlayView` поверх предыдущего контента
- `ErrorContentView` для ошибок сети
- Локализация (en/ru)

---

## Выполненные этапы

| Этап        | Описание                                                                            |
|-------------|-------------------------------------------------------------------------------------|
| Локализация | Строковые ресурсы в `values/` и `values-ru/`                                        |
| UI State    | `SearchUserUiState.kt` (Initial, Loading, Success, Empty, NetworkError)             |
| ViewModel   | `ISearchUserViewModel.kt`, `SearchUserViewModel.kt` с валидацией и логикой повторов |
| DI          | `searchUserViewModelFactory()` в `AppContainer.kt`                                  |
| UI экран    | `SearchUserScreen.kt` с `displayState` логикой и `ErrorContentView`                 |
| Preview     | `SearchUserScreenPreview.kt` — 5 превью для всех состояний                          |
| Навигация   | `Screen.UserSearch.route` в `RootScreen.kt`                                         |
| Тесты       | Unit и UI тесты                                                                     |

---

## Созданные файлы

| Файл                                            | Описание               |
|-------------------------------------------------|------------------------|
| `ui/state/SearchUserUiState.kt`                 | Sealed class состояний |
| `ui/viewmodel/ISearchUserViewModel.kt`          | Интерфейс ViewModel    |
| `ui/viewmodel/SearchUserViewModel.kt`           | Реализация ViewModel   |
| `ui/screens/profile/SearchUserScreen.kt`        | Compose экран          |
| `ui/screens/profile/SearchUserScreenPreview.kt` | 5 Preview              |
| `test/.../SearchUserViewModelTest.kt`           | Unit-тесты             |
| `androidTest/.../SearchUserScreenTest.kt`       | UI-тесты               |

## Изменённые файлы

- `res/values/strings.xml`, `res/values-ru/strings.xml` — локализация
- `data/AppContainer.kt` — фабрика ViewModel
- `ui/screens/RootScreen.kt` — навигация

---

## Технические детали

**Компоненты:** `SWTextField`, `UserRowView`, `LoadingOverlayView`, `ErrorContentView`

**API:** `UserProfileRepository.findUsers(name: String): Result<List<User>>`

**Навигация:** `Screen.UserSearch.createRoute(source)` используется как минимум в двух сценариях:

- из профиля: `source = "profile"`;
- из сообщений: `source = "messages"`.

При переходе в `OtherUserProfile` экран сохраняет исходный `source`, чтобы `AppState` корректно восстановил активную вкладку.

**displayState логика:**
- `Loading` → показывает `lastVisibleState` под оверлеем
- Иначе → показывает `uiState` напрямую (для корректной работы Preview)
